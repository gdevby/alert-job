package by.gdev.alert.job.parser.service.playwright;


import by.gdev.alert.job.parser.domain.db.Category;
import by.gdev.alert.job.parser.domain.db.Order;
import by.gdev.alert.job.parser.domain.db.ParserSource;
import by.gdev.alert.job.parser.domain.db.Subcategory;
import by.gdev.alert.job.parser.repository.CurrencyRepository;
import by.gdev.alert.job.parser.repository.OrderRepository;
import by.gdev.alert.job.parser.repository.ParserSourceRepository;
import by.gdev.alert.job.parser.service.ParserService;
import by.gdev.alert.job.parser.service.order.AbsctractSiteParser;
import by.gdev.alert.job.parser.util.proxy.ProxyCredentials;
import by.gdev.common.model.OrderDTO;
import by.gdev.common.model.SourceSiteDTO;
import com.microsoft.playwright.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.select.Elements;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;


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

    public List<OrderDTO> mapItemsWithRetry(String link, boolean proxyActive,
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

                if (!result.isEmpty()) {
                    return result;
                }
                //log.warn("Попытка {}: пустой результат, пробуем снова", attempt);
            }catch (PlaywrightException e) {
            if (e.getMessage() != null && e.getMessage().contains("Timeout")) {
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
                .filter(order -> getParserService().isExistsOrder(category, subCategory, order.getLink()))
                .filter(order -> !orderRepository.existsByLinkCategoryAndSubCategory(
                        order.getLink(),
                        category.getId(),
                        subCategory != null ? subCategory.getId() : null
                ))
                .map(order -> saveOrder(order, category, subCategory))
                .map(order -> getOrderData(order, category, subCategory))
                .toList();
    }

    protected abstract List<OrderDTO> mapPlaywrightItems(String link, Long siteSourceJobId, Category c, Subcategory sub);
}
