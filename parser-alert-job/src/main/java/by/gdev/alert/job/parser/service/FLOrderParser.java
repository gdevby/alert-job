package by.gdev.alert.job.parser.service;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.transaction.Transactional;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
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
public class FLOrderParser {

	private final WebClient webClient;
	private final ParserService service;
	private final SiteSourceJobRepository siteSourceJobRepository;
	private final OrderRepository orderRepository;
	private final ParserSourceRepository parserSourceRepository;

	private Pattern paymentPatter = Pattern.compile(".*[Бб]юджет: ([0-9]+).*");
	private final ModelMapper mapper;

	
	@Transactional
	public List<OrderDTO> flruParser() {
		log.trace("parsed fl.ru");
		List<OrderDTO> orders = new ArrayList<>();
//		// find elements from database with Hubr.ru name
		SiteSourceJob siteSourceJob = siteSourceJobRepository.findByName("FLRU");
		siteSourceJob.getCategories().stream()
				// parse only categories that can parse=true
				.filter(categoryFilter -> categoryFilter.isParse())
				// iterate over each category from this collection
				.forEach(category -> {
					log.trace("getting order by category {} rss link {}", category.getNativeLocName(), category.getLink());
					List<Subcategory> siteSubCategories = category.getSubCategories();
					// checking if a subcategory exists for this category
						// category does't have a subcategory
						List<OrderDTO> list = flruMapItems(category.getLink(), siteSourceJob.getId(), category, null);
						orders.addAll(list);
						// category have a subcategory
						siteSubCategories.stream()
								// parse only sub categories that can parse=true
								.filter(subCategoryFilter -> subCategoryFilter.isParse())
								// Iterate all sub category
								.forEach(subCategory -> {
									log.trace("getting order by category {} and subcategory  {} {}",
											category.getNativeLocName(), subCategory.getNativeLocName(), subCategory.getLink());
									List<OrderDTO> list1 = flruMapItems(subCategory.getLink(), siteSourceJob.getId(),
											category, subCategory);
									orders.addAll(list1);
								});
				});
		return orders;
	}
	
	@SneakyThrows
	private List<OrderDTO> flruMapItems(String rssURI, Long siteSourceJobId, Category category, Subcategory subCategory) {
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
				}).filter(e -> e.isValidOrder())
				.map(m -> {
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
				})
				.peek(e -> log.debug("found new order {} {}", e.getTitle(), e.getLink())).collect(Collectors.toList());
	}
	
	@SneakyThrows
	private Order parsePrice(Order order) {
		Matcher m = paymentPatter.matcher(order.getTitle());
		if (m.find()) {
			order.setPrice(new Price("", Integer.valueOf(m.group(1))));
		}
		Document doc = null;
		try {
			doc = Jsoup.parse(new URL(order.getLink()), 30000);
		} catch (IOException ex) {
			order.setValidOrder(false);
			log.debug("invalid flru link " + order.getLink());
			return order;
		}
		Element el = doc.selectFirst(".b-layout__txt_lineheight_1");
		if (Objects.nonNull(el) && (el.text().contains("Срочный заказ") || el.text().contains("Для всех"))) {
			order.setFlRuForAll(true);
		}
		return order;
	}
}