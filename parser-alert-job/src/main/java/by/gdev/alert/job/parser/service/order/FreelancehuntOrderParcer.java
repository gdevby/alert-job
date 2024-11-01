package by.gdev.alert.job.parser.service.order;

import by.gdev.alert.job.parser.domain.currency.Currency;
import by.gdev.alert.job.parser.domain.db.Category;
import by.gdev.alert.job.parser.domain.db.CurrencyEntity;
import by.gdev.alert.job.parser.domain.db.Order;
import by.gdev.alert.job.parser.domain.db.ParserSource;
import by.gdev.alert.job.parser.domain.db.Price;
import by.gdev.alert.job.parser.domain.db.Subcategory;
import by.gdev.alert.job.parser.repository.CurrencyRepository;
import by.gdev.alert.job.parser.repository.OrderRepository;
import by.gdev.alert.job.parser.repository.ParserSourceRepository;
import by.gdev.alert.job.parser.service.ParserService;
import by.gdev.alert.job.parser.util.SiteName;
import by.gdev.common.model.OrderDTO;
import by.gdev.common.model.SourceSiteDTO;
import com.google.common.collect.Lists;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FreelancehuntOrderParcer extends AbsctractSiteParser {

    @Value("${freelancehunt.proxy.active}")
    private boolean isNeedProxy;

    private static final String DATE_FORMAT = "d MMMM yyyy";

    private static final int CURRENTYEAR = LocalDate.now().getYear();

    private final ParserService service;
    private final OrderRepository orderRepository;
    private final ParserSourceRepository parserSourceRepository;
    private final CurrencyRepository currencyRepository;
    private final ModelMapper mapper;

    private RestTemplate restTemplate;

    @Transactional(timeout = 2000)
    public List<OrderDTO> parse() {
        return super.getOrders(5L);
    }

    @Override
    @SneakyThrows
    public List<OrderDTO> mapItems(String link, Long siteSourceJobId, Category category, Subcategory subCategory) {
        if (Objects.isNull(link))
            return Lists.newArrayList();
        HttpHeaders headers = new HttpHeaders();
        headers.add("user-agent", "Application");
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        restTemplate = getRestTemplate(isNeedProxy);
        ResponseEntity<String> res = restTemplate.exchange(link, HttpMethod.GET, entity, String.class);
        Document doc = Jsoup.parse(res.getBody());
        Element full = doc.getElementsByClass("col-md-9 col-md-push-3").get(0);
        Element elements = full.selectFirst("table.table.table-normal.project-list");
        Element table = elements.selectFirst("tbody");
        return table.children().stream()
                .map(e -> {
                    Order order = new Order();
                    Element info = e.selectFirst("td.left");
                    Element titlePart = info.selectFirst("a.visitable");
                    order.setTitle(titlePart.text());
                    order.setLink(titlePart.attr("href"));
                    Element description = info.selectFirst("p");
                    order.setMessage(description.text());
                    Element datePart = e.selectFirst("td.text-center.hidden-xs > div.with-tooltip > div.with-tooltip.calendar");
                    if (Objects.isNull(datePart)) {
                        order.setDateTime(new Date());
                    } else {
                        String pageDate = datePart.attr("title");
                        String orderDate = String.format("%s %s", pageDate, CURRENTYEAR);
                        Date date = generateOrderDate(orderDate);
                        order.setDateTime(date);
                    }
                    Element pricePart = e.selectFirst("td.text-center.project-budget.hidden-xs > span > div.text-green.price");
                    if (Objects.nonNull(pricePart)) {
                        CurrencyEntity ce = currencyRepository.findByCurrencyCode(Currency.UAH.name()).get();
                        String price = pricePart.text();
                        String pricaValue = price.replace("\u202FUAH", "");
                        pricaValue = pricaValue.replace("\u202F", "");
                        Double priceValue = (Integer.valueOf(pricaValue) / ce.getNominal()) * ce.getCurrencyValue();
                        Price p = new Price(price, priceValue.intValue());
                        order.setPrice(p);
                    }
                    ParserSource parserSource = new ParserSource();
                    parserSource.setSource(siteSourceJobId);
                    parserSource.setCategory(category.getId());
                    parserSource.setSubCategory(subCategory.getId());
                    order.setSourceSite(parserSource);
                    return order;
                })
                .filter(Order::isValidOrder)
                .filter(f -> service.isExistsOrder(category, subCategory, f.getLink()))
                .map(e -> {
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
                    if (Objects.nonNull(subCategory))
                        source.setSubCategoryName(subCategory.getNativeLocName());
                    dto.setSourceSite(source);
                    return dto;
                })
                .toList();
    }

    private Date generateOrderDate(String orderDate) {
        try {
            DateFormat format = new SimpleDateFormat(DATE_FORMAT, new Locale("ru"));
            return format.parse(orderDate);
        } catch (ParseException e) {
            log.error("problem with convert date {}", orderDate);
            return new Date();
        }
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.FREELANCEHUNT;
    }
}