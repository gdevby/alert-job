package by.gdev.alert.job.parser.service.order;

import by.gdev.alert.job.parser.domain.db.Category;
import by.gdev.alert.job.parser.domain.db.Order;
import by.gdev.alert.job.parser.domain.db.ParserSource;
import by.gdev.alert.job.parser.domain.db.Price;
import by.gdev.alert.job.parser.domain.db.Subcategory;
import by.gdev.alert.job.parser.domain.rss.Rss;
import by.gdev.alert.job.parser.repository.OrderRepository;
import by.gdev.alert.job.parser.repository.ParserSourceRepository;
import by.gdev.alert.job.parser.repository.SiteSourceJobRepository;
import by.gdev.alert.job.parser.service.ParserService;
import by.gdev.alert.job.parser.util.SiteName;
import by.gdev.common.model.OrderDTO;
import by.gdev.common.model.SourceSiteDTO;
import com.google.common.collect.Lists;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class HabrOrderParser extends AbsctractSiteParser {

	private final ParserService service;
	private final SiteSourceJobRepository siteSourceJobRepository;
	private final OrderRepository orderRepository;
	private final ParserSourceRepository parserSourceRepository;

	private final ModelMapper mapper;

	@Transactional(timeout = 2000)
	public List<OrderDTO> parse() {
		return super.getOrders(2L);
	}

	@Override
	@SneakyThrows
    protected List<OrderDTO> mapItems(String rssURI, Long siteSourceJobId, Category category, Subcategory subCategory) {
		JAXBContext jaxbContext = JAXBContext.newInstance(Rss.class);
		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		Rss rss = (Rss) jaxbUnmarshaller.unmarshal(new URL(rssURI));
		return Objects.isNull(rss.getChannel().getItem()) ? Lists.newArrayList()
				: rss.getChannel().getItem().stream()
						.filter(f -> service.isExistsOrder(category, subCategory, f.getLink())).map(m -> {
							log.debug("found new order {} {}", m.getTitle(), m.getLink());
							service.saveOrderLinks(category, subCategory, m.getLink());
							Order order = new Order();
							order.setTitle(m.getTitle());
							order.setDateTime(m.getPubDate());
							order.setMessage(m.getDescription());
							order.setLink(m.getLink());
							order = parsePrice(order);
							ParserSource parserSource = new ParserSource();
							parserSource.setSource(siteSourceJobId);
							parserSource.setCategory(category.getId());
							parserSource.setSubCategory(Objects.nonNull(subCategory) ? subCategory.getId() : null);
							order.setSourceSite(parserSource);
							return order;
						}).filter(e -> e.isValidOrder()).map(m -> {

							ParserSource parserSource = m.getSourceSite();
							Optional<ParserSource> optionalSource = parserSourceRepository
									.findBySourceAndCategoryAndSubCategory(parserSource.getSource(),
											parserSource.getCategory(), parserSource.getSubCategory());
							if (optionalSource.isPresent()) {
								parserSource = optionalSource.get();
							} else {
								parserSource = parserSourceRepository.save(parserSource);
							}
							m.setSourceSite(parserSource);
							m = orderRepository.save(m);
							OrderDTO dto = mapper.map(m, OrderDTO.class);
							SourceSiteDTO source = dto.getSourceSite();
							source.setCategoryName(category.getNativeLocName());
							if (Objects.nonNull(subCategory))
								source.setSubCategoryName(subCategory.getNativeLocName());
							dto.setSourceSite(source);
							return dto;
						}).collect(Collectors.toList());
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
			order.setTechnologies(
					elements.eachText().stream().filter(e -> e.length() < 200).collect(Collectors.toList()));
		} catch (Exception ex) {
			order.setValidOrder(false);
			log.debug("invalid hubr link " + order.getLink());
			return order;
		}
		return order;
	}

	@Override
	public SiteName getSiteName() {
		return SiteName.HABR;
	}
}