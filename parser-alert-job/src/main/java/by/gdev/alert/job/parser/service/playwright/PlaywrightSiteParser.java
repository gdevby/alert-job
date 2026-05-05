package by.gdev.alert.job.parser.service.playwright;


import by.gdev.alert.job.parser.domain.db.*;
import by.gdev.alert.job.parser.repository.CurrencyRepository;
import by.gdev.alert.job.parser.repository.OrderRepository;
import by.gdev.alert.job.parser.repository.ParserSourceRepository;
import by.gdev.alert.job.parser.repository.SiteSourceJobRepository;
import by.gdev.alert.job.parser.service.ParserService;
import by.gdev.alert.job.parser.service.order.AbsctractSiteParser;
import by.gdev.alert.job.parser.util.Pair;
import by.gdev.alert.job.parser.util.proxy.ProxyCredentials;
import by.gdev.common.model.OrderDTO;
import by.gdev.common.model.SourceSiteDTO;
import com.microsoft.playwright.*;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;


@Slf4j
@Component
@RequiredArgsConstructor
public abstract class PlaywrightSiteParser extends AbsctractSiteParser {

    @Autowired
    private PlaywrightManager playwrightManager;

    @Value("${parser.site.retry.attempts:3}")
    private int retryAttempts;

    @Value("${parser.site.retry.delay:2000}")
    private long retryDelayMs;

    @Value("${parser.retry.ignored-errors:}")
    private String ignoredErrorsRaw;

    private List<String> ignoredErrors;

    @Getter
    @Autowired
    private ParserSourceRepository parserSourceRepository;

    @Getter
    @Autowired
    private SiteSourceJobRepository siteSourceJobRepository;

    @Getter
    @Autowired
    private ParserService parserService;

    @Getter
    @Autowired
    private OrderRepository orderRepository;

    @Getter
    @Autowired
    private CurrencyRepository currencyRepository;

    @Autowired
    private ModelMapper mapper;

    protected boolean debug;

    protected boolean headless;


    @PostConstruct
    private void initIgnoredErrors() {
        if (ignoredErrorsRaw == null || ignoredErrorsRaw.isBlank()) {
            ignoredErrors = List.of();
            return;
        }

        ignoredErrors = Arrays.stream(ignoredErrorsRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        log.debug("Игнорируемые ошибки Playwright: {}", ignoredErrors);
    }

    protected ProxyCredentials getProxyWithRetry(int maxRetries, long retryDelayMs) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                ProxyCredentials proxy = playwrightManager.getProxyWithRetry(maxRetries, retryDelayMs);
                if (proxy != null) {;
                    return proxy;
                }
            } catch (Exception e) {
                log.error("Error getting proxy on attempt {} for {}: {}",
                        attempt, getSiteName(), e.getMessage(), e);
            }
        }
        return null;
    }

    public Playwright createPlaywright() {
        return playwrightManager.createPlaywright();
    }

    protected void closeResources(Page page, BrowserContext context, Browser browser, Playwright playwright) {
        playwrightManager.closeResources(page, context , browser, playwright, getSiteName());
    }

    protected BrowserContext createBrowserContext(Browser browser, ProxyCredentials proxy, boolean useProxy) {
        return playwrightManager.createBrowserContext(browser, proxy, useProxy, getSiteName());
    }

    protected Browser createBrowser(Playwright playwright, ProxyCredentials proxy, boolean headless, boolean isActiveProxy){
        return playwrightManager.createBrowser(playwright, proxy, headless, isActiveProxy, getSiteName());
    }

    /**
     * Класс для хранения сессии Playwright
     */
    @Getter
    @AllArgsConstructor
    protected static class PlaywrightSession {
        private final Playwright playwright;
        private final Browser browser;
        private final BrowserContext context;
        private final Page page;
        private final ProxyCredentials proxy;
    }

    /**
     * Создает и возвращает сессию Playwright
     */
    protected PlaywrightSession createSession(boolean headless, boolean useProxy) {
        Playwright playwright = createPlaywright();
        ProxyCredentials proxy = useProxy ? getProxyWithRetry(5, 2000) : null;

        Browser browser = createBrowser(playwright, proxy, headless, useProxy);
        BrowserContext context = createBrowserContext(browser, proxy, useProxy);
        Page page = context.newPage();
        return new PlaywrightSession(playwright, browser, context, page, proxy);
    }

    @Override
    public List<OrderDTO> parse() {
        Long siteId = getSiteName().getId();
        SiteSourceJob siteSourceJob = siteSourceJobRepository.findWithCategories(siteId);
        if (siteSourceJob != null) {
            return getOrdersForPlayright(siteSourceJob);
        }
        return List.of();
    }

    public List<OrderDTO> getOrdersForPlayright(SiteSourceJob siteSourceJob) {
        List<Pair<Category, Subcategory>>  categoriesPairList = getCategoriesPairListForJob(siteSourceJob);
        return mapItems(siteSourceJob.getParsedURI(), siteSourceJob.getId(), categoriesPairList);
    }

    List<Pair<Category, Subcategory>> getCategoriesPairListForJob(SiteSourceJob siteSourceJob){
        List<Pair<Category, Subcategory>> categoriesPairList = new ArrayList<>();
        siteSourceJob.getCategories().forEach(category -> {
            List<Subcategory> siteSubCategories = category.getSubCategories();
            if (category.isParse()) {
                Pair<Category, Subcategory> categoryPair = new Pair<>(category, null);
                categoriesPairList.add(categoryPair);
            }
            siteSubCategories.stream()
                    .filter(Subcategory::isParse)
                    .forEach(subCategory -> {
                        Pair<Category, Subcategory> subCategoryPair = new Pair<>(category, subCategory);
                        categoriesPairList.add(subCategoryPair);
                    });
        });
        return categoriesPairList;
    }

    private List<OrderDTO> executeWithRetry(Supplier<List<OrderDTO>> operation,
                                            String operationDescription) {
        Exception lastIgnored = null;
        Exception lastNonIgnored = null;

        for (int attempt = 1; attempt <= retryAttempts; attempt++) {
            try {
                log.debug("Попытка {}/{} {}",
                        attempt, retryAttempts, operationDescription);
                return operation.get();
            } catch (PlaywrightException e) {
                boolean isIgnored = ignoredErrors.stream()
                        .anyMatch(err -> e.getMessage() != null && e.getMessage().contains(err));

                if (isIgnored) {
                    lastIgnored = e;
                    continue;
                }

                lastNonIgnored = e;
                log.error("Playwright ошибка на попытке {} для {}: {}",
                        attempt, getSiteName(), e.getMessage());
            } catch (Exception e) {
                lastNonIgnored = e;
                log.error("Неожиданная ошибка на попытке {} для {}: {}",
                        attempt, getSiteName(), e.getMessage());
            }
            // Задержка между попытками
            if (attempt < retryAttempts) {
                try {
                    long delay = retryDelayMs * (long) Math.pow(2, attempt - 1);
                    Thread.sleep(delay);
                } catch (InterruptedException ignored) {
                }
            }
        }

        if (lastNonIgnored != null) {
            log.error("Все {} попытки {} провалились. Последняя ошибка: {}",
                    retryAttempts, operationDescription, lastNonIgnored.getMessage());
            throw new RuntimeException("Все попытки " + operationDescription + " провалились", lastNonIgnored);
        }

        if (lastIgnored != null) {
            log.warn("Все {} попытки {} завершились предупреждениями. Последнее: {}",
                    retryAttempts, operationDescription, lastIgnored.getMessage());;
        }

        return List.of();
    }

    // Создаем две версии метода mapItemsWithRetry
    public List<OrderDTO> mapItemsWithRetry(String link, boolean proxyActive,
                                            Long siteSourceJobId, Category category, Subcategory subCategory) {

        if (!active) return List.of();

        String description = String.format("парсинга %s: категория '%s', подкатегория '%s', прокси %s",
                getSiteName(),
                category.getNativeLocName(),
                subCategory != null ? subCategory.getNativeLocName() : "нет",
                proxyActive ? "включен" : "выключен");

        return executeWithRetry(
                () -> mapPlaywrightItems(link, siteSourceJobId, category, subCategory),
                description
        );
    }

    public List<OrderDTO> mapItemsWithRetry(String link, boolean proxyActive,
                                            Long siteSourceJobId, Pair<Category, Subcategory> pair, Page page) {

        if (!active) return List.of();

        Category category = pair.getLeft();
        Subcategory subCategory = pair.getRight();

        String description = String.format("парсинга %s: категория '%s', подкатегория '%s', прокси %s",
                getSiteName(),
                category.getNativeLocName(),
                subCategory != null ? subCategory.getNativeLocName() : "нет",
                proxyActive ? "включен" : "выключен");

        return executeWithRetry(
                () -> mapPlaywrightItems(link, siteSourceJobId, pair, page),
                description
        );
    }

    protected final Order saveOrder(Order order, Category category, Subcategory subCategory) {
        parserService.saveOrderLinks(category, subCategory, order.getLink());
        ParserSource ps = order.getSourceSite();
        ParserSource existing = parserSourceRepository
                .findBySourceAndCategoryAndSubCategory(ps.getSource(), ps.getCategory(), ps.getSubCategory())
                .orElseGet(() -> parserSourceRepository.save(ps));
        order.setSourceSite(existing);
        return orderRepository.save(order);
    }

    private OrderDTO getOrderData(Order order, Category category, Subcategory subCategory){
        OrderDTO dto = mapper.map(order, OrderDTO.class);
        SourceSiteDTO source = dto.getSourceSite();
        source.setCategoryName(category.getNativeLocName());
        if (subCategory != null)
            source.setSubCategoryName(subCategory.getNativeLocName());
        dto.setSourceSite(source);
        return dto;
    }

    protected List<OrderDTO> getOrdersData(List<Order> orders, Category category, Subcategory subCategory){
        return orders.stream()
                .filter(Objects::nonNull)
                .filter(Order::isValidOrder)
                .peek(order -> {
                    if (debug) {
                        log.info("*** order: site {},  title {} , link {}",
                                getSiteName(),
                                order.getTitle(),
                                order.getLink());
                    }
                })
                .filter(order -> getParserService().isExistsOrder(category, subCategory, order.getLink()))
                .map(order -> saveOrder(order, category, subCategory))
                .map(order -> getOrderData(order, category, subCategory))
                .toList();
    }


    protected abstract List<OrderDTO> mapItems(String link, Long siteSourceJobId, List<Pair<Category, Subcategory>> categoriesPairList);

    protected abstract List<OrderDTO> mapPlaywrightItems(String link, Long siteSourceJobId, Pair<Category, Subcategory> pair, Page page);

    protected abstract List<OrderDTO> mapPlaywrightItems(String link, Long siteSourceJobId, Category c, Subcategory sub);
}
