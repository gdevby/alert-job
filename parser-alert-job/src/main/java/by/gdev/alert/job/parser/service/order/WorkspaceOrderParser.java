package by.gdev.alert.job.parser.service.order;

import by.gdev.alert.job.parser.domain.db.Category;
import by.gdev.alert.job.parser.domain.db.Order;
import by.gdev.alert.job.parser.domain.db.ParserSource;
import by.gdev.alert.job.parser.domain.db.Price;
import by.gdev.alert.job.parser.domain.db.Subcategory;
import by.gdev.alert.job.parser.service.order.jsoup.JsoupClient;
import by.gdev.alert.job.parser.util.SiteName;
import by.gdev.common.model.OrderDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
@Slf4j
@RequiredArgsConstructor
public class WorkspaceOrderParser extends AbsctractSiteParser {

    private final JsoupClient jsoupClient;

    private final String baseURI = "https://workspace.ru";
    private final String statusParam = "?STATUS=published";
    
    @Value("${parser.work.workspace.ru}")
    private void setActive(boolean active) {
        this.active = active;
    }

    @Override
    protected List<OrderDTO> mapItems(String link, Long siteSourceJobId, Category category, Subcategory subCategory) {
        if (!active)
            return new ArrayList<>();

        try {
            Document document = jsoupClient.get(link + statusParam);
            Elements cards = document.getElementsByClass("vacancies__card _tender");

            List<Order> rawOrders = cards.stream()
                    .map(card -> buildOrder(card, siteSourceJobId, category, subCategory))
                    .filter(Objects::nonNull)
                    .toList();

            return getOrdersData(rawOrders, category, subCategory);
        } catch (org.jsoup.HttpStatusException e) {
            if (e.getStatusCode() == 403) {
                log.warn("Workspace returned 403 for link {}", link);
                return List.of();
            }
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Order buildOrder(Element card, Long siteSourceJobId, Category category, Subcategory subCategory) {
        Element element = card.children().get(1);
        String title = element.child(0).child(0).text();
        String postfixLink = element.child(0).child(0).attr("href");
        String price = element.child(1).text();
        String date = card.children().get(2).child(0).child(1).text();
        String fullLink = baseURI + postfixLink;

        if (!getParserService().isExistsOrder(category, subCategory, fullLink))
            return null;

        Order order = getOrderRepository().findOrdersByLink(fullLink).stream().findFirst().orElseGet(Order::new);

        order.setTitle(title);
        order.setLink(fullLink);

        try {
            SimpleDateFormat formatter =
                    new SimpleDateFormat("dd MMMM yyyy", Locale.forLanguageTag("ru"));
            order.setDateTime(formatter.parse(date));
        } catch (ParseException e) {
            log.warn("Cannot parse date {} for Workspace", date);
            order.setDateTime(new Date());
        }

        // Цена
        Matcher matcher = Pattern.compile("(\\d+(?:\\s\\d+)*)").matcher(price);
        if (matcher.find()) {
            int value = Integer.parseInt(matcher.group(1).replaceAll("\\s", ""));
            order.setPrice(new Price(price, value));
        }

        ParserSource ps = new ParserSource();
        ps.setSource(siteSourceJobId);
        ps.setCategory(category.getId());
        ps.setSubCategory(subCategory != null ? subCategory.getId() : null);
        order.setSourceSite(ps);
        return order;
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.WORKSPACE;
    }
}