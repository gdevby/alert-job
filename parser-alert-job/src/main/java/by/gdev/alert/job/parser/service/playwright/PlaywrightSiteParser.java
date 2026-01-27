package by.gdev.alert.job.parser.service.playwright;


import by.gdev.alert.job.parser.domain.db.Category;
import by.gdev.alert.job.parser.domain.db.Subcategory;
import by.gdev.alert.job.parser.service.order.AbsctractSiteParser;
import by.gdev.alert.job.parser.util.SiteName;
import by.gdev.alert.job.parser.util.proxy.ProxyCredentials;
import by.gdev.common.model.OrderDTO;
import com.microsoft.playwright.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;


@Slf4j
public abstract class PlaywrightSiteParser extends AbsctractSiteParser {

    @Autowired
    private PlaywrightManager playwrightManager;

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

    protected void closePageResources(Page page, BrowserContext context, Playwright playwright, Browser browser) {
        playwrightManager.closePageResources(page, context , browser, playwright,  getSiteName());
    }

    protected BrowserContext createBrowserContext(Browser browser, ProxyCredentials proxy, boolean useProxy) {
        return playwrightManager.createBrowserContext(browser, proxy, useProxy);
    }

    protected Playwright createPlaywright(){
        return playwrightManager.createPlaywright();
    }

    protected Browser createBrowser(Playwright playwright, ProxyCredentials proxy, boolean isActiveProxy){
        return playwrightManager.createBrowser(playwright, proxy, isActiveProxy, getSiteName());
    }

    public List<OrderDTO> mapItemsWithRetry(String link, SiteName site, boolean proxyActive,
                                            Long siteSourceJobId, Category category, Subcategory subCategory) {
        if (!active)
            return List.of();

        Exception lastError = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                log.info("Попытка {}/3 парсинга {}: категория '{}', подкатегория '{}', прокси {}",
                        attempt,
                        site,
                        category.getNativeLocName(),
                        subCategory != null ? subCategory.getNativeLocName() : "нет",
                        proxyActive ? "включен" : "выключен");

                List<OrderDTO> result = mapPlaywrightItems(link, siteSourceJobId, category, subCategory);

                if (!result.isEmpty()) {
                    return result;
                }

                log.warn("Попытка {}: пустой результат, пробуем снова", attempt);

            } catch (Exception e) {
                lastError = e;
                log.warn("Попытка {}: ошибка парсинга сайта {}: {}", attempt, site, e.getMessage());
            }
        }

        log.error("Все 3 попытки парсинга {} провалились. Последняя ошибка: {}", site,
                lastError != null ? lastError.getMessage() : "неизвестно");
        return List.of();
    }

    protected abstract List<OrderDTO> mapPlaywrightItems(String link, Long siteSourceJobId, Category c, Subcategory sub);
}
