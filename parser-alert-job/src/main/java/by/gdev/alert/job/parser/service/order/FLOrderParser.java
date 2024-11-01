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
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FLOrderParser extends AbsctractSiteParser {

	private final ParserService service;
	private final SiteSourceJobRepository siteSourceJobRepository;
	private final OrderRepository orderRepository;
	private final ParserSourceRepository parserSourceRepository;

	private Pattern paymentPatter = Pattern.compile(".*[Бб]юджет: (\\d+).*");
	private Pattern currencyPatter = Pattern.compile("\\d.*&#8381;");

	private final ModelMapper mapper;

	@Transactional(timeout = 2000)
	public List<OrderDTO> parse() {
		return super.getOrders(1L);
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
							String title = order.getTitle().replaceAll("(\\(Бюджет: .*[0-9\\;\\)])", "");
							order.setTitle(title);
							ParserSource parserSource = new ParserSource();
							parserSource.setSource(siteSourceJobId);
							parserSource.setCategory(category.getId());
							parserSource.setSubCategory(Objects.nonNull(subCategory) ? subCategory.getId() : null);
							order.setSourceSite(parserSource);
							return order;
						}).filter(Order::isValidOrder).map(m -> {
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
						}).peek(e -> log.debug("found new order {} {}", e.getTitle(), e.getLink()))
						.collect(Collectors.toList());
	}

	@SneakyThrows
	private Order parsePrice(Order order) {
		Matcher m = paymentPatter.matcher(order.getTitle());
		Price price = new Price();
		if (m.find()) {
			price.setValue(Integer.valueOf(m.group(1)));
			order.setPrice(price);
		}
		Matcher m1 = currencyPatter.matcher(order.getTitle());
		if (m1.find()) {
			price.setPrice(m1.group(0).replaceAll("&#8381;", "руб."));
			order.setPrice(price);
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
			order.setOpenForAll(true);
		}
		return order;
	}


	public SiteName getSiteName() {
		return SiteName.FLRU;
	}


}