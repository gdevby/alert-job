package by.gdev.alert.job.parser.service;

import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;

import by.gdev.alert.job.parser.domain.db.Category;
import by.gdev.alert.job.parser.domain.db.Order;
import by.gdev.alert.job.parser.domain.db.ParserSource;
import by.gdev.alert.job.parser.domain.db.Price;
import by.gdev.alert.job.parser.domain.db.Subcategory;
import by.gdev.alert.job.parser.repository.OrderRepository;
import by.gdev.alert.job.parser.repository.ParserSourceRepository;
import by.gdev.common.model.OrderDTO;
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
public class WeblancerOrderParcer extends AbsctractSiteParser {

	private final ParserService service;
	private final OrderRepository orderRepository;
	private final ParserSourceRepository parserSourceRepository;

	private final ModelMapper mapper;

	private final String sourceLink = "https://www.weblancer.net";
	private static final String DATE_FORMAT = "EEE, dd.MM.yyyy HH:mm";

	private SimpleDateFormat convertor = new SimpleDateFormat(DATE_FORMAT);

	@Transactional(timeout = 200)
	public List<OrderDTO> weblancerParser() {
		return super.getOrders(4L);
	}

	@Override
	@SneakyThrows
	public List<OrderDTO> mapItems(String link, Long siteSourceJobId, Category category, Subcategory subCategory) {
		if (Objects.isNull(link))
			return Lists.newArrayList();
		DateFormatSymbols dfs = DateFormatSymbols.getInstance(new Locale("ru"));
		String[] shortWeekdays = { "", "Вс", "Пн", "Вт", "Ср", "Чт", "Пт", "Сб" };
		dfs.setShortWeekdays(shortWeekdays);
		convertor.setDateFormatSymbols(dfs);

		Document doc = Jsoup.connect(link).get();
		Element full = doc.getElementsByClass("cols_table divided_rows").get(0);

		return full.children().stream().map(e -> {
			Order order = new Order();
			Element titleElement = e.selectFirst("div.title");
			String titleText = titleElement.text();
			order.setTitle(titleText);
			String orderPage = titleElement.selectFirst("a[href]").attr("href");
			String orderLink = sourceLink.concat(orderPage);
			order.setLink(orderLink);
			Element descriptionElement = e.selectFirst("div.text_field.text-inline");
			String descriptionText = descriptionElement.text();
			order.setMessage(descriptionText);
			String dateOrder = e.selectFirst("span.text-muted").children().attr("title");
			order.setDateTime(dateConvertor(dateOrder));
			String priceElement = e.selectFirst("div.float-right.float-sm-none.title.amount.indent-xs-b0").children()
					.attr("title");
			if (!StringUtils.isEmpty(priceElement)) {
				String[] sp = priceElement.split("•");
				Price p = new Price(sp[1], Integer.valueOf(sp[1].replaceAll(" ", "").replaceAll("руб", "")));
				order.setPrice(p);
			}
			ParserSource parserSource = new ParserSource();
			parserSource.setSource(siteSourceJobId);
			parserSource.setCategory(category.getId());
			parserSource.setSubCategory(subCategory.getId());
			order.setSourceSite(parserSource);
			return order;
		}).filter(f -> service.isExistsOrder(category, subCategory, f.getLink())).map(e -> {
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
			return mapper.map(e, OrderDTO.class);
		}).peek(e -> log.debug("found new order {} {}", e.getTitle(), e.getLink())).toList();
	}

	private Date dateConvertor(String d) {
		try {
			return convertor.parse(d);
		} catch (ParseException e) {
			log.error("problem with convert date {}", d);
			return new Date();
		}
	}
}
