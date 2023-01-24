package by.gdev.alert.job.parser.service;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.transaction.Transactional;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import by.gdev.alert.job.parser.domain.db.Category;
import by.gdev.alert.job.parser.domain.db.SiteSourceJob;
import by.gdev.alert.job.parser.domain.db.Subcategory;
import by.gdev.alert.job.parser.domain.rss.Rss;
import by.gdev.alert.job.parser.repository.SiteSourceJobRepository;
import by.gdev.common.model.Order;
import by.gdev.common.model.Price;
import by.gdev.common.model.SourceSiteDTO;
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

	@Transactional
	public List<Order> hubrParser() {
		List<Order> orders = new ArrayList<>();
//		// find elements from database with Hubr.ru name
		SiteSourceJob siteSourceJob = siteSourceJobRepository.findByName("HABR");
		siteSourceJob.getCategories().stream()
				// parse only categories that can parse=true
				.filter(categoryFilter -> categoryFilter.isParse())
				// iterate over each category from this collection
				.forEach(category -> {
					log.trace("getting order by category {}", category.getNativeLocName());
					Set<Subcategory> siteSubCategories = category.getSubCategories();
					// category does't have a subcategory
					List<Order> list = flruMapItems(category.getLink(), siteSourceJob.getId(), category, null);
					orders.addAll(list);
					// category have a subcategory
					siteSubCategories.stream()
							// parse only sub categories that can parse=true
							.filter(subCategoryFilter -> subCategoryFilter.isParse())
							// Iterate all sub category
							.forEach(subCategory -> {
								log.trace("getting order by category {} and subcategory  {}",
										category.getNativeLocName(), subCategory.getNativeLocName());
								List<Order> list2 = flruMapItems(subCategory.getLink(), siteSourceJob.getId(), category,
										subCategory);
								orders.addAll(list2);
							});
				});
		return orders;
	}

	@SneakyThrows
	private List<Order> flruMapItems(String rssURI, Long siteSourceJobId, Category category, Subcategory subCategory) {
		JAXBContext jaxbContext = JAXBContext.newInstance(Rss.class);
		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		Rss rss = (Rss) jaxbUnmarshaller.unmarshal(new URL(rssURI));
		return rss.getChannel().getItem().stream()
				.filter(f -> service.isExistsOrder(category, subCategory, f.getLink())).map(m -> {
					service.saveOrderLinks(category, subCategory, m.getLink());
					Order o = new Order();
					o.setTitle(m.getTitle().toLowerCase());
					o.setDateTime(m.getPubDate());
					o.setMessage(m.getDescription().toLowerCase());
					o.setLink(m.getLink());
					parsePrice(o);
					SourceSiteDTO dto = new SourceSiteDTO();
					dto.setSource(siteSourceJobId);
					dto.setCategory(category.getId());
					dto.setSubCategory(Objects.nonNull(subCategory) ? subCategory.getId() : null);
					dto.setFlRuForAll(o.isFlRuForAll());
					o.setSourceSite(dto);
					return o;
				}).filter(e -> e.isValidOrder()).peek(e -> {
					log.debug("found new order {} {}", e.getTitle(), e.getLink());
				}).collect(Collectors.toList());
	}

	private Order parsePrice(Order order) {
		Document doc = null;
		try {
			doc = Jsoup.parse(new URL(order.getLink()), 30000);
		} catch (IOException ex) {
			order.setValidOrder(false);
			log.debug("invalid hubr link " + order.getLink());
			return order;
		}
		Element el = doc.selectFirst("span.count");
		Element elPaymentType = el.selectFirst(".suffix");
		if (Objects.nonNull(elPaymentType) && elPaymentType.text().equals("за проект")) {
			String s = el.childNode(0).toString();
			order.setPrice(new Price(s, Integer.valueOf(s.replaceAll(" ", "").replaceAll("руб.", ""))));
		}
		Elements elements = doc.select(".tags__item_link");
		order.setTechnologies(elements.eachText().stream().map(e -> e.toLowerCase()).collect(Collectors.toList()));
		order.setValidOrder(true);
		return order;
	}
}