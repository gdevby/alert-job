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
import by.gdev.common.model.OrderDTO;
import by.gdev.common.model.SourceSiteDTO;
import com.google.common.collect.Lists;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Data
@EqualsAndHashCode(callSuper = false)
@Service
@RequiredArgsConstructor
@Slf4j
public class FreelanceRuOrderParser extends AbsctractSiteParser {

	private final ParserService service;
	private final SiteSourceJobRepository siteSourceJobRepository;
	private final OrderRepository orderRepository;
	private final ParserSourceRepository parserSourceRepository;

	private Pattern paymentPatter = Pattern.compile(".*[Бб]юджет: ([0-9]+).*");
	private Pattern currencyPatter = Pattern.compile("[0-9].*&#8381;");

	private final ModelMapper mapper;
	private volatile Map<String, String> cookies;
	private volatile LocalDateTime lastLogin;
	@Value("${relogin.freelance.every.minutes}")
	private Long reloginEveryMinutes;
	@Value("${freelance.ru.account.login}")
	private String login;
	@Value("${freelance.ru.account.password}")
	private String password;
	@Value("${timeout.connect_read.order}")
	private int timeout;

	@Transactional(timeout = 2000)
	public List<OrderDTO> getOrders() {
		return super.getOrders(3L);
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
							order.setLink(m.getLink());
							order = parseExtraFields(order, m.getDescription());
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

	@SneakyThrows
	private Order parseExtraFields(Order order, String priceRaw) {
		Matcher m = paymentPatter.matcher(priceRaw);
		Price price = new Price();
		if (m.find()) {
			price.setValue(Integer.valueOf(m.group(1)));
			order.setPrice(price);
		}
		Matcher m1 = currencyPatter.matcher(priceRaw);
		if (m1.find()) {
			price.setPrice(m1.group(0).replaceAll("&#8381;", "руб."));
			order.setPrice(price);
		}

		if (Objects.isNull(lastLogin) || lastLogin.isBefore(LocalDateTime.now())) {
			if (login.isBlank() || password.isBlank()) {
				log.info("empty in properties login password for FreelanceRu to get description of the order");
			}
			Connection.Response loginForm = Jsoup.connect("https://freelance.ru/login/?return_url=%2F")
					.method(Connection.Method.GET).timeout(timeout).execute();
			Map<String, String> map = loginForm.cookies();
			Connection.Response loginCookie = Jsoup.connect("https://freelance.ru/login/").timeout(timeout)
					.data("passwd", password).data("login", login).data("check_ip", "on").data("return_url", "/")
					.data("auth", "auth").cookies(map).execute();
			cookies = loginCookie.cookies();
			lastLogin = LocalDateTime.now().plusMinutes(reloginEveryMinutes);
		}
		Document doc = null;
		try {
			doc = Jsoup.connect(order.getLink()).timeout(timeout).cookies(cookies).get();
			order.setMessage(doc.select(".txt.set-href-auto").html());
		} catch (IOException ex) {
			order.setValidOrder(false);
			log.debug("invalid flru link " + order.getLink());
			return order;
		}
		return order;
	}
}