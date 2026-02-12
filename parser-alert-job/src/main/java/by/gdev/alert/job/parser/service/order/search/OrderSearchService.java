package by.gdev.alert.job.parser.service.order.search;

import by.gdev.alert.job.parser.domain.db.Order;
import by.gdev.alert.job.parser.repository.OrderSearchRepository;
import by.gdev.alert.job.parser.repository.SiteSourceJobRepository;
import by.gdev.alert.job.parser.service.order.search.converter.Converter;
import by.gdev.alert.job.parser.service.order.search.dto.OrderSearchRequest;
import by.gdev.alert.job.parser.service.order.search.dto.PageResponse;
import by.gdev.common.model.OrderDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderSearchService {

    private final OrderSearchRepository orderSearchRepository;
    private final SiteSourceJobRepository siteSourceJobRepository;
    private final Converter<Order, OrderDTO> orderDtoConverter;

    public PageResponse<OrderDTO> search(OrderSearchRequest req) {

        long start = System.nanoTime();

        Long siteId = siteSourceJobRepository.findByName(req.getSite()).getId();
        int offset = req.getPage() * req.getSize();
        String booleanQuery = buildBooleanQueryOr(req.getKeywords());

        List<Order> orders = orderSearchRepository.searchOrders(
                siteId,
                req.getCategoryId(),
                req.getSubCategoryId(),
                booleanQuery,
                offset,
                req.getSize()
        );

        long total = orderSearchRepository.countOrders( siteId, req.getCategoryId(), req.getSubCategoryId(), booleanQuery );
        int totalPages = (int) Math.ceil((double) total / req.getSize());

        long executionTimeMs = (System.nanoTime() - start) / 1_000_000;

        log.debug("Время выполнения запроса: {} мс", executionTimeMs);

        return new PageResponse<>(orderDtoConverter.convertAll(orders),
                req.getPage(),
                req.getSize(),
                total,
                totalPages,
                req.getPage() == 0,
                req.getPage() + 1 >= totalPages);
    }

    private String buildBooleanQueryAnd(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return null;
        }

        return keywords.stream()
                .filter(k -> k != null && !k.isBlank())
                .map(k -> "+" + k.trim())
                .reduce("", (a, b) -> a + " " + b)
                .trim();
    }


    private String buildBooleanQueryOr(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return null;
        }

        return keywords.stream()
                .filter(k -> k != null && !k.isBlank())
                .map(String::trim)
                .reduce("", (a, b) -> a + " " + b)
                .trim();
    }


    private OrderDTO convert(Order order){
        return orderDtoConverter.convert(order);
    }
}
