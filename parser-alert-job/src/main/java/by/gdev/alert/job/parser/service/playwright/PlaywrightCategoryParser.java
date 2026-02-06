package by.gdev.alert.job.parser.service.playwright;

import by.gdev.alert.job.parser.domain.db.SiteSourceJob;
import by.gdev.alert.job.parser.service.Parser;
import by.gdev.alert.job.parser.service.category.ParsedCategory;
import by.gdev.alert.job.parser.util.proxy.ProxyCredentials;
import com.microsoft.playwright.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Map;

@Slf4j
public abstract class PlaywrightCategoryParser implements Parser {

    @Autowired
    private PlaywrightManager playwrightManager;

    @Value("${parser.category.retry.attempts:3}")
    private int retryAttempts;

    @Value("${parser.category.retry.delay:2000}")
    private long retryDelayMs;

    public ProxyCredentials getProxyWithRetry(int maxRetries, long retryDelayMs) {
        return playwrightManager.getProxyWithRetry(maxRetries, retryDelayMs);
    }

    protected Browser createBrowser(Playwright playwright, ProxyCredentials proxy, boolean headless, boolean isActiveProxy){
        return playwrightManager.createBrowser(playwright, proxy, headless, isActiveProxy, getSiteName());
    }

    public void closeResources(Page page, BrowserContext context, Browser browser, Playwright playwright) {
        playwrightManager.closeResources(page, context , browser, playwright, getSiteName());
    }

    public Playwright createPlaywright() {
        return playwrightManager.createPlaywright();
    }

    protected BrowserContext createBrowserContext(Browser browser, ProxyCredentials proxy, boolean useProxy) {
        return playwrightManager.createBrowserContext(browser, proxy, useProxy);
    }

    public Map<ParsedCategory, List<ParsedCategory>> parseWithRetry(SiteSourceJob job) {
        Exception lastError = null;

        for (int attempt = 1; attempt <= retryAttempts; attempt++) {
            try {
                log.info("Попытка {}/{} парсинга категорий {}", attempt, retryAttempts, getSiteName());
                return parsePlaywright(job);
            } catch (PlaywrightException e) {
                if (e.getMessage() != null && e.getMessage().contains("Timeout")) {
                    continue;
                }
                lastError = e;
                log.error("Playwright ошибка на попытке {} для {}: {}", attempt, getSiteName(), e.getMessage());
            } catch (Exception e) {
                lastError = e;
                log.error("Неожиданная ошибка на попытке {} для {}: {}", attempt, getSiteName(), e.getMessage());
            }
        }

        if (lastError == null) {
            log.warn("Все 3 попытки парсинга категорий {} дали пустой результат", getSiteName());
            return Map.of();
        }

        log.error("Все {} попытки парсинга категорий {} провалились. Последняя ошибка: {}",
                retryAttempts,
                getSiteName(),
                lastError.getMessage());

        return Map.of();
    }

    protected abstract Map<ParsedCategory, List<ParsedCategory>> parsePlaywright(SiteSourceJob job);
}
