package by.gdev.alert.job.parser.service;

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

import by.gdev.alert.job.parser.domain.EnumSite;
import by.gdev.alert.job.parser.domain.SiteSubCategory;
import by.gdev.alert.job.parser.model.Order;
import by.gdev.alert.job.parser.model.Price;
import by.gdev.alert.job.parser.model.Rss;
import by.gdev.alert.job.parser.repository.SiteSourceJobRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@Data
@Service
@RequiredArgsConstructor
public class HabrOrderParser {

	private final WebClient webClient;
	private final ParserUtil util;
	private final SiteSourceJobRepository siteSourceJobRepository;

	@Transactional
	public List<Order> hubrParser() {
		List<Order> orders = new ArrayList<>();
//		// find all elements from database with Hubr.ru name
		siteSourceJobRepository.findAllByName(EnumSite.HUBR.name()).forEach(element -> {
			element.getSiteCategories().stream()
					// parse only categories that can parse=true
					.filter(categoryFilter -> categoryFilter.isParse())
					// iterate over each category from this collection
					.forEach(categories -> {
						Set<SiteSubCategory> siteSubCategories = categories.getSiteSubCategories();
						// checking if a subcategory exists for this category
						if (Objects.isNull(siteSubCategories)) {
							// category does't have a subcategory
							orders.addAll(hubrMapItems(categories.getLink()));
						} else {
							// category have a subcategory
							siteSubCategories.stream()
									// parse only sub categories that can parse=true
									.filter(subCategoryFilter -> subCategoryFilter.isParse())
									// Iterate all sub category
									.forEach(subCategories -> {
										orders.addAll(hubrMapItems(subCategories.getLink()));
									});
						}
					});
		});
		return orders;
	}

	@SneakyThrows
	private List<Order> hubrMapItems(String rssURI) {
		JAXBContext jaxbContext = JAXBContext.newInstance(Rss.class);
		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		Rss rss = (Rss) jaxbUnmarshaller.unmarshal(new URL(rssURI));
		return rss.getChannel().getItem().stream().filter(util.orderFilter()::test).map(m -> {
			Order o = new Order();
			o.setTitle(m.getTitle().toLowerCase());
			o.setDateTime(m.getPubDate());
			o.setMessage(m.getDescription().toLowerCase());
			o.setLink(m.getLink());
			parsePrice(o);
			return o;
		}).collect(Collectors.toList());
	}

	@SneakyThrows
	private void parsePrice(Order order) {
		Document doc = Jsoup.parse(new URL(order.getLink()), 30000);
		Element el = doc.selectFirst("span.count");
		Element elPaymentType = el.selectFirst(".suffix");
		if (Objects.nonNull(elPaymentType) && elPaymentType.text().equals("за проект")) {
			String s = el.childNode(0).toString();
			order.setPrice(new Price(s, Integer.valueOf(s.replaceAll(" ", "").replaceAll("руб.", ""))));
		}
		Elements elements = doc.select(".tags__item_link");
		order.setTechnologies(elements.eachText().stream().map(e -> e.toLowerCase()).collect(Collectors.toList()));
	}
}