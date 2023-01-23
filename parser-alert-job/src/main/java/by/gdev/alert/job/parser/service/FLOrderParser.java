package by.gdev.alert.job.parser.service;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.transaction.Transactional;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
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

@Data
@Service
@RequiredArgsConstructor
public class FLOrderParser {

	private final WebClient webClient;
	private final ParserService service;
	private final SiteSourceJobRepository siteSourceJobRepository;

	private Pattern paymentPatter = Pattern.compile(".*[Бб]юджет: ([0-9]+).*");

	
	@Transactional
	public List<Order> flruParser() {
		List<Order> orders = new ArrayList<>();
//		// find elements from database with Hubr.ru name
		SiteSourceJob siteSourceJob = siteSourceJobRepository.findByName("FLRU");
		siteSourceJob.getCategories().stream()
				// parse only categories that can parse=true
				.filter(categoryFilter -> categoryFilter.isParse())
				// iterate over each category from this collection
				.forEach(categories -> {
					Set<Subcategory> siteSubCategories = categories.getSubCategories();
					// checking if a subcategory exists for this category
						// category does't have a subcategory
						List<Order> list = flruMapItems(categories.getLink(), siteSourceJob.getId(), categories, null);
						orders.addAll(list);
						// category have a subcategory
						siteSubCategories.stream()
								// parse only sub categories that can parse=true
								.filter(subCategoryFilter -> subCategoryFilter.isParse())
								// Iterate all sub category
								.forEach(subCategories -> {
									List<Order> list1 = flruMapItems(subCategories.getLink(), siteSourceJob.getId(),
											categories, subCategories);
									orders.addAll(list1);
								});
				});
		return orders;
	}
	
	@SneakyThrows
	private List<Order> flruMapItems(String rssURI, Long siteSourceJobId, Category category, Subcategory subCategory) {
		JAXBContext jaxbContext = JAXBContext.newInstance(Rss.class);
		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		Rss rss = (Rss) jaxbUnmarshaller.unmarshal(new URL(rssURI));
		return rss.getChannel().getItem().stream().filter(f -> service.isExistsOrder(category, subCategory, f.getLink()))
				.map(m -> {
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
				}).collect(Collectors.toList());
	}
	
	@SneakyThrows
	private void parsePrice(Order order) {
		Matcher m = paymentPatter.matcher(order.getTitle());
		if (m.find()) {
			order.setPrice(new Price("", Integer.valueOf(m.group(1))));
		}
		Document doc = Jsoup.parse(new URL(order.getLink()), 30000);
		Element el = doc.selectFirst(".b-layout__txt_lineheight_1");
		if (Objects.nonNull(el) && (el.text().contains("Срочный заказ") || el.text().contains("Для всех"))) {
			order.setFlRuForAll(true);
		}
	}
}