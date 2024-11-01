package by.gdev.alert.job.parser.service.order;

import by.gdev.alert.job.parser.domain.YouDoRoot;
import by.gdev.alert.job.parser.domain.db.Category;
import by.gdev.alert.job.parser.domain.db.Order;
import by.gdev.alert.job.parser.domain.db.ParserSource;
import by.gdev.alert.job.parser.domain.db.Price;
import by.gdev.alert.job.parser.domain.db.Subcategory;
import by.gdev.alert.job.parser.repository.OrderRepository;
import by.gdev.alert.job.parser.repository.ParserSourceRepository;
import by.gdev.alert.job.parser.repository.SiteSourceJobRepository;
import by.gdev.alert.job.parser.service.ParserService;
import by.gdev.alert.job.parser.util.SiteName;
import by.gdev.common.model.OrderDTO;
import by.gdev.common.model.SourceSiteDTO;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class YouDoOrderParcer extends AbsctractSiteParser {

	@Value("${youdo.proxy.active}")
	private boolean isNeedProxy;


	private final String youDoLink = "https://youdo.com/api/tasks/tasks/";
	private final String youDoOrderLink = "https://youdo.com";
	private final String regex = "[а-яА-Я\\.\\s]";

	private final ParserService service;
	private final SiteSourceJobRepository siteSourceJobRepository;
	private final OrderRepository orderRepository;
	private final ParserSourceRepository parserSourceRepository;
	private RestTemplate restTemplate;
	private final ModelMapper mapper;

	@Transactional(timeout = 2000)
	public List<OrderDTO> parse() {
		return super.getOrders(6L);
	}

	@Override
	public List<OrderDTO> mapItems(String link, Long siteSourceJobId, Category category, Subcategory subCategory) {
		String body = generateRequestBody(link);
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.ALL));
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<>(body, headers);
		restTemplate = getRestTemplate(isNeedProxy);
		ResponseEntity<YouDoRoot> r = restTemplate.postForEntity(youDoLink, entity, YouDoRoot.class);
		List<String> urls = r.getBody().getResultObject().getItems().stream()
				.map(e -> String.format(youDoOrderLink + "%s", e.getUrl())).toList();
		return urls.stream().map(url -> {
			Order order = new Order();
			Document doc = connect(url);
			Element element = doc.getElementById("TaskContainer");
			Element title = element.selectFirst("h1.b-task-block__header__title");
			order.setTitle(title.text());
			order.setLink(url);
			Element description = element.selectFirst("div.b-task-block__description.js-descriptionResult");
			if (Objects.nonNull(description)) {
				order.setMessage(description.text());
			}
			order.setDateTime(new Date());
			Element priceBlock = element.selectFirst("div.b-task-block__header__price");
			Element priceValue = priceBlock.selectFirst("span.b-price-label-new > span.js-price-label");
			String price = priceValue.text().replaceAll(regex, "");
			Price p = new Price(priceValue.text(), Integer.valueOf(price));
			order.setPrice(p);
			ParserSource parserSource = new ParserSource();
			parserSource.setSource(siteSourceJobId);
			parserSource.setCategory(category.getId());
			parserSource.setSubCategory(Objects.nonNull(subCategory) ? subCategory.getId() : null);
			order.setSourceSite(parserSource);
			return order;
		}).filter(Order::isValidOrder).filter(f -> service.isExistsOrder(category, subCategory, f.getLink())).map(e -> {
			log.debug("found new order {} {}", e.getTitle(), e.getLink());
			service.saveOrderLinks(category, subCategory, e.getLink());
			ParserSource parserSource = e.getSourceSite();
			Optional<ParserSource> optionalSource = parserSourceRepository.findBySourceAndCategoryAndSubCategory(
					parserSource.getSource(), parserSource.getCategory(), parserSource.getSubCategory());
			if (optionalSource.isPresent()) {
				parserSource = optionalSource.get();
			} else {
				parserSource = parserSourceRepository.save(parserSource);
			}
			e.setSourceSite(parserSource);
			e = orderRepository.save(e);
			OrderDTO dto = mapper.map(e, OrderDTO.class);
			SourceSiteDTO source = dto.getSourceSite();
			source.setCategoryName(category.getNativeLocName());
			if (Objects.nonNull(subCategory)) {
				source.setSubCategoryName(subCategory.getNativeLocName());
			}
			dto.setSourceSite(source);
			return dto;
		}).toList();
	}

	@SneakyThrows
	private Document connect(String url) {
		return Jsoup.connect(url).get();
	}

	private String generateRequestBody(String link) {
		String condition = link.equals("all") ? "\"categories\":[\"%s\"]" : "\"sub\":[%s]";
		String field = String.format(condition, link);
		String b = "{\"q\":\"\",\"list\":\"all\",\"status\":\"opened\",\"radius\":null,"
				+ "\"lat\":55.755864,\"lng\":37.617698,\"page\":1,\"noOffers\":false,\"onlySbr\":false,"
				+ "\"onlyB2B\":false,\"onlyVacancies\":false,\"priceMin\":\"\",\"sortType\":1,"
				+ "\"onlyVirtual\":false, %s,\"searchRequestId\":\"%s\"}";
		return String.format(b, field, UUID.randomUUID().toString());
	}

	@Override
	public SiteName getSiteName() {
		return SiteName.YOUDO;
	}
}