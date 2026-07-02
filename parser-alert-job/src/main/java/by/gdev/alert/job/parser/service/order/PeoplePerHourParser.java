package by.gdev.alert.job.parser.service.order;

import by.gdev.alert.job.parser.domain.db.Category;
import by.gdev.alert.job.parser.domain.db.Order;
import by.gdev.alert.job.parser.domain.db.ParserSource;
import by.gdev.alert.job.parser.domain.db.Price;
import by.gdev.alert.job.parser.domain.db.Subcategory;
import by.gdev.common.model.OrderDTO;
import by.gdev.common.model.SiteName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.HttpStatusException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import by.gdev.alert.job.parser.service.order.jsoup.JsoupClient;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;


@Service
@Slf4j
@RequiredArgsConstructor
public class PeoplePerHourParser extends AbsctractSiteParser {

    private final JsoupClient jsoupClient;

    @Value("${peopleperhour.proxy.active}")
    private boolean isNeedProxy;

    private final String url = "https://www.peopleperhour.com/services";
    public static final String CURRENCY_CODE = "USD";

    private Map<String, String> urlMapping = Map.of(
            "AI Services", "artificial+intelligence",
            "Video, Photo & Image", "video-photography"
    );
    
    @Value("${parser.work.peopleperhour.com}")
    private void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public List<OrderDTO> mapItems(String link, Long siteSourceJobId, Category category, Subcategory subCategory) {
		if (!active)
			return new ArrayList<>();
        String uri = buildUri(category, subCategory);
        Document document;
        try {
            document = jsoupClient.get(uri);
            if (document == null){
                return List.of();
            }
        } catch (HttpStatusException e) {
            if (e.getStatusCode() == 404) {
                log.warn("404 Not Found for URL {}", uri);
                return List.of();
            }
            log.error("HTTP error {} for URL {}", e.getStatusCode(), uri);
            return List.of();
        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().contains("Read timed out")) {
                log.debug("Ignored timeout for {}: {}", uri, e.getMessage());
            } else {
                log.error("IO error while parsing {}: {}", uri, e.getMessage());
            }
            return List.of();
        }

        Elements cards = document.getElementsByClass("card⤍HourlieTile⤚3DrJs");

        List<Order> rawOrders = cards.stream()
                .map(card -> buildOrder(card, siteSourceJobId, category, subCategory))
                .filter(Objects::nonNull)
                .toList();

        return getOrdersData(rawOrders, category, subCategory);
    }

    private Order buildOrder(Element card,
                             Long siteSourceJobId,
                             Category category,
                             Subcategory subCategory) {
        String title = card.getElementsByClass("title-nano card__title⤍HourlieTile⤚5LQtW").text();
        String orderLink = card.getElementsByClass("card__title-link⤍HourlieTile⤚13loh").attr("href");
        String price = card.getElementsByClass("u-txt--right card__price⤍HourlieTileMeta⤚3su1s").text();

        if (orderLink == null || orderLink.isBlank())
            return null;

        if (!getParserService().isExistsOrder(category, subCategory, orderLink))
            return null;

        Order order = getOrderRepository().findByLink(orderLink).orElse(new Order());

        order.setTitle(title);
        order.setLink(orderLink);
        order.setDateTime(new Date());

        String amount = price.substring(1).replace(",", "");

        getCurrencyRepository()
                .findByCurrencyCode(CURRENCY_CODE)
                .ifPresent(currency -> {
                    double convertedPrice =
                            (Integer.parseInt(amount) / currency.getNominal()) * currency.getCurrencyValue();
                    order.setPrice(new Price(price, (int) convertedPrice));
                });

        ParserSource parserSource = new ParserSource();
        parserSource.setSource(siteSourceJobId);
        parserSource.setCategory(category.getId());
        parserSource.setSubCategory(subCategory != null ? subCategory.getId() : null);
        order.setSourceSite(parserSource);
        return order;
    }

    private String buildUri(Category category, Subcategory subCategory) {
        String uri = url;
        if (urlMapping.containsKey(category.getNativeLocName())) {
            uri += "/" + urlMapping.get(category.getNativeLocName());
        } else {
            uri += "/" + category.getNativeLocName()
                    .toLowerCase()
                    .replaceAll("[\\s&,]+", "-");
        }
        if (subCategory != null) {
            uri += "/" + subCategory.getNativeLocName()
                    .toLowerCase()
                    .replaceAll("[\\s&,]+", "-");
        }
        return uri;
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.PEOPLEPERHOUR;
    }
}
