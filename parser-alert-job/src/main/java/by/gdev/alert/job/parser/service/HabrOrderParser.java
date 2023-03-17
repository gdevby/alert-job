package by.gdev.alert.job.parser.service;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.transaction.Transactional;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import by.gdev.alert.job.parser.domain.db.Category;
import by.gdev.alert.job.parser.domain.db.Order;
import by.gdev.alert.job.parser.domain.db.ParserSource;
import by.gdev.alert.job.parser.domain.db.Price;
import by.gdev.alert.job.parser.domain.db.SiteSourceJob;
import by.gdev.alert.job.parser.domain.db.Subcategory;
import by.gdev.alert.job.parser.domain.rss.Rss;
import by.gdev.alert.job.parser.repository.OrderRepository;
import by.gdev.alert.job.parser.repository.ParserSourceRepository;
import by.gdev.alert.job.parser.repository.SiteSourceJobRepository;
import by.gdev.common.model.OrderDTO;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Data
@Service
@RequiredArgsConstructor
@Slf4j
public class HabrOrderParser {

	private final WebClient webClient;
	private final ParserService service;
	private final SiteSourceJobRepository siteSourceJobRepository;
	private final OrderRepository orderRepository;
	private final ParserSourceRepository parserSourceRepository;

	private final ModelMapper mapper;

	@Transactional
	public List<OrderDTO> hubrParser() {
		log.trace("parsed habr");
		List<OrderDTO> orders = new ArrayList<>();
//		// find elements from database with Hubr.ru name
		SiteSourceJob siteSourceJob = siteSourceJobRepository.findByName("HABR");
		siteSourceJob.getCategories().stream()
				// parse only categories that can parse=true
				.filter(categoryFilter -> categoryFilter.isParse())
				// iterate over each category from this collection
				.forEach(category -> {
					log.trace("getting order by category {}", category.getNativeLocName());
					List<Subcategory> siteSubCategories = category.getSubCategories();
					// category does't have a subcategory
					List<OrderDTO> list = hubrMapItems(category.getLink(), siteSourceJob.getId(), category, null);
					orders.addAll(list);
					// category have a subcategory
					siteSubCategories.stream()
							// parse only sub categories that can parse=true
							.filter(subCategoryFilter -> subCategoryFilter.isParse())
							// Iterate all sub category
							.forEach(subCategory -> {
								log.trace("getting order by category {} and subcategory  {}",
										category.getNativeLocName(), subCategory.getNativeLocName());
								List<OrderDTO> list2 = hubrMapItems(subCategory.getLink(), siteSourceJob.getId(),
										category, subCategory);
								orders.addAll(list2);
							});
				});
		return orders;
	}

	@SneakyThrows
	private List<OrderDTO> hubrMapItems(String rssURI, Long siteSourceJobId, Category category,
			Subcategory subCategory) {
		JAXBContext jaxbContext = JAXBContext.newInstance(Rss.class);
		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		Rss rss = (Rss) jaxbUnmarshaller.unmarshal(new URL(rssURI));
		return rss.getChannel().getItem().stream()
				.filter(f -> service.isExistsOrder(category, subCategory, f.getLink())).map(m -> {
					service.saveOrderLinks(category, subCategory, m.getLink());
					Order order = new Order();
					order.setTitle(m.getTitle().toLowerCase());
					order.setDateTime(m.getPubDate());
					order.setMessage(m.getDescription().toLowerCase());
					order.setLink(m.getLink());
					order = parsePrice(order);
					ParserSource parserSource = new ParserSource();
					parserSource.setSource(siteSourceJobId);
					parserSource.setCategory(category.getId());
					parserSource.setSubCategory(Objects.nonNull(subCategory) ? subCategory.getId() : null);
					parserSource.setFlRuForAll(order.isFlRuForAll());
					order.setSourceSite(parserSource);
					return order;
				}).filter(e -> e.isValidOrder()).map(m -> {
					ParserSource parserSource = m.getSourceSite();
					Optional<ParserSource> optionalSource = parserSourceRepository
							.findBySourceAndCategoryAndSubCategory(parserSource.getSource(), parserSource.getCategory(),
									parserSource.getSubCategory());
					if (optionalSource.isPresent()) {
						parserSource = optionalSource.get();
					} else {
						parserSource = parserSourceRepository.save(parserSource);
					}
					m.setSourceSite(parserSource);
					m = orderRepository.save(m);
					return mapper.map(m, OrderDTO.class);
				}).peek(e -> log.debug("found new order {} {}", e.getTitle(), e.getLink()))
				.collect(Collectors.toList());
	}

	private Order parsePrice(Order order) {
		Document doc = null;
		try {
			doc = Jsoup.parse(new URL(order.getLink()), 30000);

			Element el = doc.selectFirst("span.count");
			Element elPaymentType = el.selectFirst(".suffix");
			if (Objects.nonNull(elPaymentType) && elPaymentType.text().equals("за проект")) {
				String s = el.childNode(0).toString();
				order.setPrice(new Price(s, Integer.valueOf(s.replaceAll(" ", "").replaceAll("руб.", ""))));
			}
			Elements elements = doc.select(".tags__item_link");
			order.setTechnologies(elements.eachText().stream().map(e -> e.toLowerCase()).collect(Collectors.toList()));
		} catch (Exception ex) {
			order.setValidOrder(false);
			log.debug("invalid hubr link " + order.getLink());
			return order;
		}
		return order;
	}
}