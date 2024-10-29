package by.gdev.alert.job.parser.service;

import by.gdev.alert.job.parser.domain.db.Category;
import by.gdev.alert.job.parser.domain.db.CurrencyEntity;
import by.gdev.alert.job.parser.domain.db.Order;
import by.gdev.alert.job.parser.domain.db.ParserSource;
import by.gdev.alert.job.parser.domain.db.Price;
import by.gdev.alert.job.parser.domain.db.Subcategory;
import by.gdev.alert.job.parser.repository.CurrencyRepository;
import by.gdev.alert.job.parser.repository.OrderRepository;
import by.gdev.alert.job.parser.repository.ParserSourceRepository;
import by.gdev.common.model.OrderDTO;
import by.gdev.common.model.SourceSiteDTO;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;


@Service
@Slf4j
@RequiredArgsConstructor
public class PeoplePerHourParser extends AbsctractSiteParser {

    @Value("${peopleperhour.proxy.active}")
    private boolean isNeedProxy;

    private final String url = "https://www.peopleperhour.com/services";
    public static final String CURRENCY_CODE = "USD";

    private Map<String, String> urlMapping = Map.of(
            "AI Services", "artificial+intelligence",
            "Video, Photo & Image", "video-photography"
    );

    private final CurrencyRepository currencyRepository;
    private final ParserService parserService;
    private final ParserSourceRepository parserSourceRepository;
    private final OrderRepository orderRepository;
    private final ModelMapper mapper;

    @Transactional
    public List<OrderDTO> peoplePerHourParser() {
        return super.getOrders(10L);
    }

    @Override
    public List<OrderDTO> mapItems(String link, Long siteSourceJobId, Category category, Subcategory subCategory) {

        String uri = url;
        if (urlMapping.containsKey(category.getNativeLocName())) {
            uri += "/" + urlMapping.get(category.getNativeLocName());
        }else {
            uri += "/" +category.getNativeLocName().toLowerCase().replaceAll("[\\s&,]+", "-");
        }

        if (subCategory != null) {
            uri += "/" + subCategory.getNativeLocName().toLowerCase().replaceAll("[\\s&,]+", "-");
        }
        Document document = null;
        try {
            document = Jsoup.connect(uri).get();
        } catch (IOException e) {
            log.error("Cannot parse {}", uri);
            return List.of();
        }

        Elements cards = document.getElementsByClass("card⤍HourlieTile⤚3DrJs");


        return cards.stream()
                .map(card -> {
                    String title = card.getElementsByClass("title-nano card__title⤍HourlieTile⤚5LQtW").text();
                    String orderLink = card.getElementsByClass("card__title-link⤍HourlieTile⤚13loh").attr("href");

                    Order order = new Order();
                    order.setTitle(title);
                    order.setLink(orderLink);
                    order.setDateTime(new Date());

                    String price = card.getElementsByClass("u-txt--right card__price⤍HourlieTileMeta⤚3su1s").text();
                    String amount = price.substring(1).replace(",", "");

                    Optional<CurrencyEntity> optionalCurrency = currencyRepository.findByCurrencyCode(CURRENCY_CODE);

                    optionalCurrency.ifPresent(currency -> {
                        double convertedPrice = (Integer.parseInt(amount) / currency.getNominal()) * currency.getCurrencyValue();
                        order.setPrice(new Price(price, (int) convertedPrice));
                    });

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
                    log.debug("found new order {} {}", order.getTitle(), order.getLink());
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
}
