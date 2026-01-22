package by.gdev.alert.job.parser.service.order;

import by.gdev.alert.job.parser.domain.db.*;
import by.gdev.alert.job.parser.domain.kwork.Kwork;
import by.gdev.alert.job.parser.repository.OrderRepository;
import by.gdev.alert.job.parser.repository.ParserSourceRepository;
import by.gdev.alert.job.parser.repository.SiteSourceJobRepository;
import by.gdev.alert.job.parser.service.ParserService;
import by.gdev.alert.job.parser.util.SiteName;
import by.gdev.common.model.OrderDTO;
import by.gdev.common.model.SourceSiteDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class KworkComOrderParser extends AbsctractSiteParser {

    private final ParserService service;
    private final SiteSourceJobRepository siteSourceJobRepository;
    private final OrderRepository orderRepository;
    private final ParserSourceRepository parserSourceRepository;
    private RestTemplate restTemplate;
    private final ModelMapper mapper;
    private final ObjectMapper objectMapper;

    @Value("${kworkcom.proxy.active}")
    private boolean isNeedProxy;

    private final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    @Value("${parser.work.kworkcom.com}")
    private void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.KWORKCOM;
    }

    @Override
    protected List<OrderDTO> mapItems(String link, Long siteSourceJobId, Category category, Subcategory subCategory) {
        if (!active)
            return new ArrayList<>();

        restTemplate = getRestTemplate(isNeedProxy);
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        headers.set("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8");
        headers.set("Accept-Language", "ru-RU,ru;q=0.8,en-US;q=0.5,en;q=0.3");
        headers.set("Accept-Encoding", "gzip, deflate, br");
        headers.set("Connection", "keep-alive");
        headers.set("Upgrade-Insecure-Requests", "1");
        headers.set("Sec-Fetch-Dest", "document");
        headers.set("Sec-Fetch-Mode", "navigate");
        headers.set("Sec-Fetch-Site", "none");
        headers.set("Cache-Control", "max-age=0");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        String kw = restTemplate.exchange(link, org.springframework.http.HttpMethod.GET, entity, String.class).getBody();
        Pattern pattern = Pattern.compile("\"wants\":(.+?),\"wantsFromAllRubrics");
        Matcher matcer = pattern.matcher(kw);
        Stream<Kwork> list = matcer.results().map(e -> readValue(e.group(1))).flatMap(e -> e.stream());
        return list.map(e -> {
                    Order order = new Order();
                    order.setTitle(e.getName());
                    order.setLink(String.format("https://kwork.com/projects/%s/view", e.getId()));
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
                })
                .filter(Order::isValidOrder)
                .filter(f -> service.isExistsOrder(category, subCategory, f.getLink()))
                .map(e -> {
                    //log.debug("found new order {} {}", e.getTitle(), e.getLink());
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
                })
                .toList();
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
