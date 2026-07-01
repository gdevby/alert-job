package by.gdev.alert.job.parser.service.order;

import by.gdev.alert.job.parser.domain.db.Category;
import by.gdev.alert.job.parser.domain.db.Order;
import by.gdev.alert.job.parser.domain.db.ParserSource;
import by.gdev.alert.job.parser.domain.db.Price;
import by.gdev.alert.job.parser.domain.db.Subcategory;
import by.gdev.alert.job.parser.domain.truelancer.TreuelancerProjects;
import by.gdev.alert.job.parser.domain.truelancer.TrueLancerRoot;
import by.gdev.alert.job.parser.domain.truelancer.TruelancerOrder;
import by.gdev.alert.job.parser.util.SiteName;
import by.gdev.common.model.OrderDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class TruelancerOrderParser extends AbsctractSiteParser {
    @Value("${freelancer.proxy.active}")
    private boolean isNeedProxy;
    private String uri = "https://api.truelancer.com/api/v1/projects";
    private RestTemplate restTemplate;
    @Value("${parser.work.truelancer.com}")
    private void setActive(boolean active) {
        this.active = active;
    }

    @Override
    protected List<OrderDTO> mapItems(String link, Long siteSourceJobId, Category category, Subcategory subCategory) {
		if (!active)
			return new ArrayList<>();
        restTemplate = getRestTemplate(isNeedProxy);

        TrueLancerRoot root = callApi(link);
        if (root == null) {
            return List.of();
        }
        TreuelancerProjects projects = root.getProjects();
        if (projects == null) {
            return List.of();
        }
        List<TruelancerOrder> orders = projects.getOrders();

        List<Order> rawOrders = orders.stream()
                .map(o -> buildOrder(o, siteSourceJobId, category, subCategory))
                .filter(Objects::nonNull)
                .toList();
        return getOrdersData(rawOrders, category, subCategory);
    }

    private TrueLancerRoot callApi(String link) {
        try {
            return restTemplate.postForObject(uri, Map.of("category", link), TrueLancerRoot.class);
        } catch (HttpServerErrorException e) {
            int code = e.getStatusCode().value();
            if (code == 502 || code == 503 || code == 504) {
                log.warn("{} API {} — игнорируем", getSiteName(), code);
                return null;
            }
            throw e;
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("Connect timed out")) {
                log.debug("Ignored timeout for {}", getSiteName());
                return null;
            }
            log.error("Ошибка {}: {}", getSiteName(), e.getMessage());
            return null;
        }
    }

    private Order buildOrder(TruelancerOrder tr, Long siteSourceJobId, Category category, Subcategory subCategory) {
        if (!getParserService().isExistsOrder(category, subCategory, tr.getLink()))
            return null;
        Order order = getOrderRepository().findOrdersByLink(tr.getLink()).stream().findFirst().orElseGet(Order::new);
        order.setTitle(tr.getTitle());
        order.setLink(tr.getLink());
        order.setMessage(tr.getDescription());
        order.setDateTime(new Date());
        // Цена
        String currencyCode = tr.getCurrency();
        getCurrencyRepository()
                .findByCurrencyCode(currencyCode)
                .ifPresent(c -> {
                    double converted = (tr.getBudget() / c.getNominal()) * c.getCurrencyValue();
                    order.setPrice(new Price(tr.getBudget() + " " + currencyCode, (int) converted));
                });
        ParserSource ps = new ParserSource();
        ps.setSource(siteSourceJobId);
        ps.setCategory(category.getId());
        ps.setSubCategory(subCategory != null ? subCategory.getId() : null);
        order.setSourceSite(ps);
        return order;
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.TRUELANCER;
    }
}
