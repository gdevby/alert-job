package by.gdev.alert.job.parser.service.order;

import by.gdev.alert.job.parser.domain.db.Category;
import by.gdev.alert.job.parser.domain.db.Order;
import by.gdev.alert.job.parser.domain.db.ParserSource;
import by.gdev.alert.job.parser.domain.db.Price;
import by.gdev.alert.job.parser.domain.db.Subcategory;
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
import org.jsoup.select.Elements;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FreelanceRuOrderParser extends AbsctractSiteParser {

    @Value("${relogin.freelance.every.minutes}")
    private Long reloginEveryMinutes;
    @Value("${freelance.ru.account.login}")
    private String login;
    @Value("${freelance.ru.account.password}")
    private String password;
    @Value("${timeout.connect_read.order}")
    private int timeout;

    private final ParserService service;
    private final OrderRepository orderRepository;
    private final ParserSourceRepository parserSourceRepository;
    private final ModelMapper mapper;

    private final String baseURI = "https://freelance.ru";

    private final String regex = "[а-яА-Я\\.\\s]";

    private RestTemplate restTemplate;

    @Value("${freelanceRu.proxy.active}")
    private boolean isNeedProxy;
    @Value("${parser.work.freelance.ru}")
    private void setActive(boolean active) {
        this.active = active;
    }

    @Override
    @SneakyThrows
    protected List<OrderDTO> mapItems(String link, Long siteSourceJobId, Category category, Subcategory subCategory) {
        if (Objects.isNull(link))
            return Lists.newArrayList();
        if (!active)
            return new ArrayList<>();
        HttpHeaders headers = new HttpHeaders();
        headers.add("user-agent", "Application");
        headers.setAccept(Arrays.asList(MediaType.TEXT_HTML));
        headers.setContentType(MediaType.TEXT_HTML);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        restTemplate = getRestTemplate(isNeedProxy);
        ResponseEntity<String> res = restTemplate.exchange(link, HttpMethod.GET, entity, String.class);
        Document doc = Jsoup.parse(res.getBody());
        Elements projects = doc.getElementsByClass("project");
        return projects.stream().map(project -> {
                    Order order = new Order();

                    Element title = project.selectFirst("div.box-title");
                    if (title == null) {
                        log.warn("NO TITLE BLOCK: {}", project);
                        return null;
                    }
                    Element h2 = title.selectFirst("h2.title");
                    if (h2 == null) {
                        log.warn("NO H2 TITLE: {}", project);
                        return null;
                    }

                    // CHECK TITLE FOR EMOJI
                    String titleText = h2.attr("title");
                    if (containsEmoji(titleText)) {
                        log.warn("SKIP EMOJI ORDER (TITLE): {}", titleText);
                        return null;
                    }
                    order.setTitle(titleText);

                    Element description = title.selectFirst("a.description");
                    if (description == null) {
                        log.warn("NO DESCRIPTION: {}", project);
                        return null;
                    }

                    String href = description.attr("href");
                    if (href == null || href.isBlank()) {
                        log.warn("EMPTY HREF: {}", project);
                        return null;
                    }
                    order.setLink(baseURI + href);

                    // CHECK MESSAGE FOR EMOJI
                    String msg = description.text();
                    if (containsEmoji(msg)) {
                        log.warn("SKIP EMOJI ORDER (MESSAGE): {}", msg);
                        return null;
                    }
                    order.setMessage(msg);
                    order.setDateTime(new Date());

                    Element priceElement = project.selectFirst("div.cost");
                    if (priceElement == null) {
                        log.warn("NO PRICE: {}", project);
                        return null;
                    }

                    // 🔥 CHECK PRICE FOR EMOJI
                    String rawPrice = priceElement.text();
                    if (containsEmoji(rawPrice)) {
                        log.warn("SKIP EMOJI ORDER (PRICE): {}", rawPrice);
                        return null;
                    }

                    String priceString = rawPrice.replaceAll(regex, "");
                    Integer priceValue = priceString.isBlank() ? 0 : Integer.valueOf(priceString);
                    Price p = new Price(rawPrice, priceValue);
                    order.setPrice(p);

                    ParserSource parserSource = new ParserSource();
                    parserSource.setSource(siteSourceJobId);
                    parserSource.setCategory(category.getId());
                    parserSource.setSubCategory(Objects.nonNull(subCategory) ? subCategory.getId() : null);
                    order.setSourceSite(parserSource);
                    return order;

                }).filter(Objects::nonNull)
                .filter(Order::isValidOrder)
                .filter(f -> service.isExistsOrder(category, subCategory, f.getLink())).map(e -> {
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

    private boolean containsEmoji(String text) {
        return text != null && text.codePoints().anyMatch(
                cp -> Character.getType(cp) == Character.SURROGATE
        );
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.FREELANCERU;
    }
}
