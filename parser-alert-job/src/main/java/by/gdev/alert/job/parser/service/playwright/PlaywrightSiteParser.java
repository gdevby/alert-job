package by.gdev.alert.job.parser.service.playwright;


import by.gdev.alert.job.parser.domain.db.*;
import by.gdev.alert.job.parser.repository.CurrencyRepository;
import by.gdev.alert.job.parser.repository.OrderRepository;
import by.gdev.alert.job.parser.repository.ParserSourceRepository;
import by.gdev.alert.job.parser.service.ParserService;
import by.gdev.alert.job.parser.service.order.AbsctractSiteParser;
import by.gdev.alert.job.parser.util.Pair;
import by.gdev.alert.job.parser.util.proxy.ProxyCredentials;
import by.gdev.common.model.OrderDTO;
import by.gdev.common.model.SourceSiteDTO;
import com.microsoft.playwright.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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

    @Getter
    @Autowired
    private ParserSourceRepository parserSourceRepository;

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
        return playwrightManager.createBrowserContext(browser, proxy, useProxy);
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

    @Transactional(timeout = 2000)
    @Override
    public List<OrderDTO> parse() {
        Long siteId = getSiteName().getId();
        SiteSourceJob siteSourceJob = getSiteSourceJobRepository().findById(siteId).orElse(null);
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
        Exception lastError = null;
        for (int attempt = 1; attempt <= retryAttempts; attempt++) {
            try {
                log.info("Попытка {}/{} {}",
                        attempt, retryAttempts, operationDescription);

                List<OrderDTO> result = operation.get();
                return result;

            } catch (PlaywrightException e) {
                if (e.getMessage() != null && e.getMessage().contains("Timeout")) {
                    if (debug){
                        log.warn("Timeout playwright {} {}", getSiteName(), e.getMessage());
                    }
                    continue;
                }
                lastError = e;
                log.error("Playwright ошибка на попытке {} для {}: {}",
                        attempt, getSiteName(), e.getMessage());
            } catch (Exception e) {
                lastError = e;
                log.error("Неожиданная ошибка на попытке {} для {}: {}",
                        attempt, getSiteName(), e.getMessage());
            }

            // Задержка между попытками
            if (attempt < retryAttempts) {
                try {
                    Thread.sleep(retryDelayMs);
                } catch (InterruptedException ignored) {
                }
            }
        }

        if (lastError == null) {
            return List.of();
        }

        log.error("Все {} попытки {} провалились. Последняя ошибка: {}",
                retryAttempts, operationDescription, lastError.getMessage());
        return List.of();
    }

    // Теперь создаем две версии метода
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

    /*public List<OrderDTO> mapItemsWithRetry(String link, boolean proxyActive,
                                            Long siteSourceJobId, Category category, Subcategory subCategory) {
        if (!active)
            return List.of();

        Exception lastError = null;
        for (int attempt = 1; attempt <= retryAttempts; attempt++) {
            try {
                log.info("Попытка {}/{} парсинга {}: категория '{}', подкатегория '{}', прокси {}",
                        attempt,
                        retryAttempts,
                        getSiteName(),
                        category.getNativeLocName(),
                        subCategory != null ? subCategory.getNativeLocName() : "нет",
                        proxyActive ? "включен" : "выключен");

                List<OrderDTO> result = mapPlaywrightItems(link, siteSourceJobId, category, subCategory);
                return result;
            }catch (PlaywrightException e) {
            if (e.getMessage() != null && e.getMessage().contains("Timeout")) {
                if (debug){
                    log.warn("Timeout playright {} {}", getSiteName(), e.getMessage());
                }
                continue;
            }
            lastError = e;
            log.error("Playwright ошибка на попытке {} для {}: {}", attempt, getSiteName(), e.getMessage());
            }
            catch (Exception e) {
                lastError = e;
                log.error("Неожиданная ошибка на попытке {} для {}: {}", attempt, getSiteName(), e.getMessage());
            }

            //Задержка между попытками
            if (attempt < retryAttempts) {
                try {
                    Thread.sleep(retryDelayMs);
                }
                catch (InterruptedException ignored) {
                }
            }
        }
        if (lastError == null) {
            //log.warn("Все {} попытки парсинга {} дали пустой результат", retryAttempts, getSiteName());
            return List.of();
        }

        log.error("Все {} попытки парсинга {} провалились. Последняя ошибка: {}",
                retryAttempts,
                getSiteName(),
                lastError.getMessage());
        return List.of();
    }*/

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
                        log.info("*** order: {} , result {}",
                                order.getTitle(),
                                getParserService().isExistsOrder(category, subCategory, order.getLink()));
                    }
                })
                .filter(order -> getParserService().isExistsOrder(category, subCategory, order.getLink()))
                /*.filter(order -> !orderRepository.existsByLinkCategoryAndSubCategory(
                        order.getLink(),
                        category.getId(),
                        subCategory != null ? subCategory.getId() : null
                ))*/
                .map(order -> saveOrder(order, category, subCategory))
                .map(order -> getOrderData(order, category, subCategory))
                .toList();
    }


    protected abstract List<OrderDTO> mapItems(String link, Long siteSourceJobId, List<Pair<Category, Subcategory>> categoriesPairList);

    protected abstract List<OrderDTO> mapPlaywrightItems(String link, Long siteSourceJobId, Pair<Category, Subcategory> pair, Page page);

    protected abstract List<OrderDTO> mapPlaywrightItems(String link, Long siteSourceJobId, Category c, Subcategory sub);
}
