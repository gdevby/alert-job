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

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderSearchService {

    private final OrderSearchRepository orderSearchRepository;
    private final SiteSourceJobRepository siteSourceJobRepository;
    private final Converter<Object[], OrderSearchDTO> orderConverter;

    public PageSearchResponse<OrderSearchDTO> search(OrderSearchRequest req) {
        // Сайты
        List<String> sites = req.getSites();
        List<Long> siteIds = null;

        if (sites != null && !sites.isEmpty()) {
            // Убираем дубликаты строк
            Set<String> uniqueSites = sites.stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toCollection(HashSet::new));

            siteIds = uniqueSites.stream()
                    .map(String::toLowerCase)
                    .map(siteSourceJobRepository::findByName)
                    .filter(Objects::nonNull)
                    .map(SiteSourceJob::getId)
                    .toList();
        }

        int offset = req.getPage() * req.getSize();

        String booleanQuery = buildBooleanQueryOr(req.getKeywords());
        String likeQuery = extractLikeQuery(req.getKeywords());

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

        long total = orderSearchRepository.countOrders(siteIds, req.getMode(), booleanQuery , likeQuery);
        int totalPages = (int) Math.ceil((double) total / req.getSize());

        String modeDescription;
        switch (req.getMode()) {
            case "TITLE" -> modeDescription = "по заголовку";
            case "DESCRIPTION" -> modeDescription = "по описанию";
            default -> modeDescription = "по заголовку и описанию";
        }

        log.debug(
                "Поиск заказов {}: sites='{}', keywords='{}', page={}, size={}, booleanQuery='{}', executionTimeMs={} мс",
                modeDescription,
                sites,
                req.getKeywords(),
                req.getPage(),
                req.getSize(),
                booleanQuery,
                executionTimeMs
        );

        return new PageSearchResponse<>(orderConverter.convertAll(orders),
                req.getPage(),
                req.getSize(),
                total,
                totalPages,
                req.getPage() == 0,
                req.getPage() + 1 >= totalPages);
    }

    private String buildBooleanQueryOr(List<String> keywords) {
        // Если ключевых слов нет → MATCH выполнять нельзя
        if (keywords == null || keywords.isEmpty()) return null;

        // Фильтруем только безопасные слова, которые можно передавать в MATCH AGAINST
        // isBooleanSafe — проверяет отсутствие опасных символов (+, -, >, <, ~ и т.д.)
        List<String> safe = keywords.stream()
                .filter(this::isBooleanSafe)
                .toList();

        // Если после фильтрации ничего не осталось → MATCH не выполняем
        if (safe.isEmpty()) return null;

        // Собираем строку для OR‑поиска:
        // каждое слово → trim → проверка на пустоту → добавляем '*' для префиксного поиска
        // итог: "word1* word2* word3*"
        return safe.stream()
                .map(String::trim)
                .filter(k -> !k.isBlank())
                .map(k -> k + "*") //для AND сделать .map(k -> "+" + k + "*")
                .reduce((a, b) -> a + " " + b) // объединяем через пробел
                .orElse(null);
    }

    private String extractLikeQuery(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) return null;
        // первое слово, которое содержит + или -
        return keywords.stream()
                .filter(k -> !isBooleanSafe(k))
                .findFirst()
                .orElse(null);
    }

    private boolean isBooleanSafe(String word) {
        // слова с + или - НЕ подходят для BOOLEAN MODE
        return !word.contains("+") && !word.contains("-");
    }
}
