package by.gdev.alert.job.parser.service.order.search;

import by.gdev.alert.job.parser.domain.db.SiteSourceJob;
import by.gdev.alert.job.parser.repository.OrderSearchRepository;
import by.gdev.alert.job.parser.repository.SiteSourceJobRepository;
import by.gdev.alert.job.parser.service.order.search.converter.Converter;
import by.gdev.alert.job.parser.service.order.search.dto.OrderSearchRequest;
import by.gdev.alert.job.parser.service.order.search.dto.PageSearchResponse;
import by.gdev.common.model.OrderSearchDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderSearchService {

    private final OrderSearchRepository orderSearchRepository;
    private final SiteSourceJobRepository siteSourceJobRepository;
    private final Converter<Object[], OrderSearchDTO> orderConverter;

    public PageSearchResponse<OrderSearchDTO> search(OrderSearchRequest req) {

        // -----------------------------
        // Сайты по которым ищем
        // -----------------------------
        List<String> sites = req.getSites();
        List<Long> siteIds = null;

        if (sites != null && !sites.isEmpty()) {
            Set<String> uniqueSites = sites.stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toCollection(HashSet::new));

            siteIds = uniqueSites.stream()
                    .map(siteSourceJobRepository::findByName)
                    .filter(Objects::nonNull)
                    .map(SiteSourceJob::getId)
                    .toList();
        }

        int offset = req.getPage() * req.getSize();

        // -----------------------------
        // Построение поисковых строк
        // -----------------------------
        List<String> keywords = req.getKeywords();
        String booleanQuery = null;//buildBooleanQueryOr(keywords);
        String likeQuery = extractLikeQuery(keywords); //ФРАЗОВЫЙ ПОИСК

        long start = System.nanoTime();

        List<Object[]> orders = orderSearchRepository.searchOrders(
                siteIds,
                req.getMode(),
                booleanQuery,
                likeQuery,
                offset,
                req.getSize()
        );

        long executionTimeMs = (System.nanoTime() - start) / 1_000_000;

        long total = orderSearchRepository.countOrders(
                siteIds,
                req.getMode(),
                booleanQuery,
                likeQuery
        );

        int totalPages = (int) Math.ceil((double) total / req.getSize());

        String modeDescription = switch (req.getMode()) {
            case "TITLE" -> "по заголовку";
            case "DESCRIPTION" -> "по описанию";
            default -> "по заголовку и описанию";
        };

        log.debug(
                "Поиск {}: sites='{}', keywords='{}', boolean='{}', like='{}', time={} мс",
                modeDescription,
                sites,
                req.getKeywords(),
                booleanQuery,
                likeQuery,
                executionTimeMs
        );

        return new PageSearchResponse<>(
                orderConverter.convertAll(orders),
                req.getPage(),
                req.getSize(),
                total,
                totalPages,
                req.getPage() == 0,
                req.getPage() + 1 >= totalPages
        );
    }

    // ---------------------------------------------------------
    // BOOLEAN QUERY (MATCH AGAINST)
    // ---------------------------------------------------------
    private String buildBooleanQueryOr(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) return null;

        List<String> safe = keywords.stream()
                .filter(this::isBooleanSafe)
                .toList();

        if (safe.isEmpty()) return null;

        return safe.stream()
                .map(String::trim)
                .filter(k -> !k.isBlank())
                .map(k -> k + "*")
                .collect(Collectors.joining(" "));
    }

    // ---------------------------------------------------------
    // LIKE : ФРАЗОВЫЙ ПОИСК
    // ---------------------------------------------------------
    private String extractLikeQuery(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) return null;

        // Если есть спецсимволы → вернуть первое такое слово
        Optional<String> special = keywords.stream()
                .filter(k -> !isBooleanSafe(k))
                .findFirst();

        if (special.isPresent()) {
            return special.get(); // "%c++%"
        }

        // Если несколько слов → вернуть фразу
        if (keywords.size() > 1) {
            return String.join(" ", keywords); // "%spring boot%"
        }

        // Если одно слово → вернуть его
        return keywords.get(0); // "%spring%"
    }

    private boolean isBooleanSafe(String word) {
        return !word.contains("+") && !word.contains("-");
    }
}
