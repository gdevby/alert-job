package by.gdev.alert.job.parser.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import by.gdev.alert.job.parser.configuration.RestTemplateConfigurer;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import by.gdev.alert.job.parser.domain.db.Category;
import by.gdev.alert.job.parser.domain.db.Order;
import by.gdev.alert.job.parser.domain.db.ParserSource;
import by.gdev.alert.job.parser.domain.db.Price;
import by.gdev.alert.job.parser.domain.db.Subcategory;
import by.gdev.alert.job.parser.domain.kwork.Kwork;
import by.gdev.alert.job.parser.repository.OrderRepository;
import by.gdev.alert.job.parser.repository.ParserSourceRepository;
import by.gdev.alert.job.parser.repository.SiteSourceJobRepository;
import by.gdev.common.model.OrderDTO;
import by.gdev.common.model.SourceSiteDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Data
@EqualsAndHashCode(callSuper = false)
@Service
@RequiredArgsConstructor
@Slf4j
public class KworkOrderParcer extends AbsctractSiteParser {

	@Value("${kwork.proxy.active}")
	private boolean isNeedProxy;

	private final ParserService service;
	private final SiteSourceJobRepository siteSourceJobRepository;
	private final OrderRepository orderRepository;
	private final ParserSourceRepository parserSourceRepository;
	private RestTemplate restTemplate;
	private final ModelMapper mapper;
	private final ObjectMapper objectMapper;
	private final RestTemplateConfigurer restTemplateConfigurer;


	private final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

	@Transactional(timeout = 2000)
	public List<OrderDTO> kworkParser() {
		return super.getOrders(7L);
	}

	@Override
	@SneakyThrows
	public List<OrderDTO> mapItems(String link, Long siteSourceJobId, Category category, Subcategory subCategory) {

		restTemplate = getRestTemplate(isNeedProxy);

		String kw = restTemplate.getForObject(link, String.class);
		Pattern pattern = Pattern.compile("\"wants\":(.+?),\"wantsFromAllRubrics");
		Matcher matcer = pattern.matcher(kw);
		Stream<Kwork> list = matcer.results().map(e -> readValue(e.group(1))).flatMap(e -> e.stream());
		return list.map(e -> {
			Order order = new Order();
			order.setTitle(e.getName());
			order.setLink(String.format("https://kwork.ru/projects/%s/view", e.getId()));
			order.setMessage(e.getDescription());
			Date createdDate = covertStringToDate(e.getDateCreate());
			order.setDateTime(createdDate);
			Price p = new Price(String.valueOf(e.getPossiblePriceLimit()), e.getPossiblePriceLimit());
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
	private List<Kwork> readValue(String value) {
		return objectMapper.readValue(value, new TypeReference<List<Kwork>>() {
		});
	}

	private Date covertStringToDate(String date) {
		try {
			return formatter.parse(date);
		} catch (ParseException e) {
			log.error(e.getMessage());
			return new Date();
		}
	}
}