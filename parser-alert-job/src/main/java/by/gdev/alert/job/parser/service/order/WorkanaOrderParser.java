package by.gdev.alert.job.parser.service.order;

import by.gdev.alert.job.parser.domain.db.Category;
import by.gdev.alert.job.parser.domain.db.CurrencyEntity;
import by.gdev.alert.job.parser.domain.db.Order;
import by.gdev.alert.job.parser.domain.db.ParserSource;
import by.gdev.alert.job.parser.domain.db.Price;
import by.gdev.alert.job.parser.domain.db.Subcategory;
import by.gdev.alert.job.parser.domain.workana.WorkanaOrder;
import by.gdev.alert.job.parser.domain.workana.WorkanaRoot;
import by.gdev.alert.job.parser.repository.CurrencyRepository;
import by.gdev.alert.job.parser.repository.OrderRepository;
import by.gdev.alert.job.parser.repository.ParserSourceRepository;
import by.gdev.alert.job.parser.service.ParserService;
import by.gdev.alert.job.parser.util.SiteName;
import by.gdev.common.model.OrderDTO;
import by.gdev.common.model.SourceSiteDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
@Slf4j
@RequiredArgsConstructor
public class WorkanaOrderParser extends AbsctractSiteParser {

    @Value("${workana.proxy.active}")
    private boolean isProxyNeeded;
    private String baseURL = "https://www.workana.com";
    private static final String LANGUAGE = "&language=en";

    private final String currencyPattern = "\\b([A-Z]{3})\\b";  // Pattern to match 3 capital letters e.g "text USD text" -> "USD
    private final String numberPattern = "\\d+(?:[.,]\\d+)*";   // Pattern to match number with commas or dots e.g "text 40" -> 40 or "text 1,000" -> 1,000 or "text 3.000" - > 3.000

    private final CurrencyRepository currencyRepository;
    private final ParserService parserService;
    private final ParserSourceRepository parserSourceRepository;
    private final OrderRepository orderRepository;
    private final ModelMapper mapper;
    
    @Value("${parser.work.workana.com}")
    private void setActive(boolean active) {
        this.active = active;
    }


    @Override
    protected List<OrderDTO> mapItems(String link, Long siteSourceJobId, Category category, Subcategory subCategory) {
		if (!active)
			return new ArrayList<>();
        RestTemplate restTemplate = getRestTemplate(isProxyNeeded);

        ResponseEntity<WorkanaRoot> response = restTemplate.exchange(
                link + LANGUAGE,
                HttpMethod.GET,
                new HttpEntity<>(getHeaders()),
                WorkanaRoot.class
        );

        List<WorkanaOrder> workanaOrders = response.getBody().getWorkanaOrderWrap().getWorkanaOrders();

        return workanaOrders.stream()
                .map(workanaOrder -> {

                    String titleHTML = workanaOrder.getTitle();

                    Document doc = Jsoup.parse(titleHTML);

                    Element anchorElement = doc.select("a").first();
                    String linkPostfix = doc.select("a").first().attr("href");
                    String title = anchorElement.select("span").text();

                    String descriptionHTML = workanaOrder.getDescription();

                    String description = descriptionHTML.replaceFirst("<br />.*", "");


                    Order order = new Order();
                    order.setTitle(title);
                    order.setLink(baseURL + linkPostfix);
                    order.setMessage(description);
                    order.setDateTime(new Date());

                    String budget = workanaOrder.getBudget();

                    Optional<String> extractedCurrency = extractValue(currencyPattern, budget);
                    Optional<String> extractedAmount = extractValue(numberPattern, budget);

                    if (extractedCurrency.isPresent() && extractedAmount.isPresent()) {
                        Optional<CurrencyEntity> optionalCurrency = currencyRepository.findByCurrencyCode(extractedCurrency.get());
                        double amount = convertToDouble(extractedAmount.get());

                        optionalCurrency.ifPresent(currency -> {
                            double convertedPrice = (amount / currency.getNominal()) * currency.getCurrencyValue();
                            order.setPrice(new Price((int) amount + " " + currency.getCurrencyCode(), (int) convertedPrice));
                        });
                    }

                    ParserSource parserSource = new ParserSource();
                    parserSource.setSource(siteSourceJobId);
                    parserSource.setCategory(category.getId());
                    parserSource.setSubCategory(Objects.nonNull(subCategory) ? subCategory.getId() : null);

                    order.setSourceSite(parserSource);
                    return order;
                })
                .filter(Order::isValidOrder)
                .filter(order -> parserService.isExistsOrder(category, subCategory, order.getLink()))
                .map(order -> {
                    //log.debug("found new order {} {}", order.getTitle(), order.getLink());
                    parserService.saveOrderLinks(category, subCategory, order.getLink());
                    ParserSource parserSource = order.getSourceSite();
                    Optional<ParserSource> source = parserSourceRepository.findBySourceAndCategoryAndSubCategory(
                            parserSource.getSource(),
                            parserSource.getCategory(),
                            parserSource.getSubCategory()
                    );

                    if (source.isPresent()) {
                        parserSource = source.get();
                    } else {
                        parserSource = parserSourceRepository.save(parserSource);
                    }

                    order.setSourceSite(parserSource);
                    order = orderRepository.save(order);
                    OrderDTO orderDto = mapper.map(order, OrderDTO.class);
                    SourceSiteDTO sourceSiteDto = orderDto.getSourceSite();
                    sourceSiteDto.setCategoryName(category.getNativeLocName());
                    if (Objects.nonNull(subCategory))
                        sourceSiteDto.setSubCategoryName(subCategory.getNativeLocName());
                    orderDto.setSourceSite(sourceSiteDto);
                    return orderDto;
                })
                .toList();
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.WORKANA;
    }

    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();

        headers.set("sec-ch-ua-platform", "\"Linux\"");
        headers.set("Referer", "https://www.workana.com/jobs?language=en");
        headers.set("X-Requested-With", "XMLHttpRequest");
        headers.set("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36");
        headers.set("Accept", "application/json, text/plain, */*");
        headers.set("sec-ch-ua", "\"Not?A_Brand\";v=\"99\", \"Chromium\";v=\"130\"");
        headers.set("sec-ch-ua-mobile", "?0");

        return headers;
    }

    private Optional<String> extractValue(String pattern, String value) {
        Pattern currencyRegex = Pattern.compile(pattern);
        Matcher currencyMatcher = currencyRegex.matcher(value);

        if (currencyMatcher.find()) {
            return Optional.of(currencyMatcher.group());
        }
        return Optional.empty();
    }

    private double convertToDouble(String value) {
        String s = removeSpecialSymbols(value);
        return Double.parseDouble(s);
    }

    private String removeSpecialSymbols(String value) {
        if (value.contains(","))
            return value.replace(",", "");
        else if (value.contains("."))
            return value.replace(".", "");
        return value;
    }
}
