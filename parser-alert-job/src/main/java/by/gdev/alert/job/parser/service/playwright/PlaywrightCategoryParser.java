package by.gdev.alert.job.parser.service.playwright;

import by.gdev.alert.job.parser.domain.db.SiteSourceJob;
import by.gdev.alert.job.parser.repository.CategoryRepository;
import by.gdev.alert.job.parser.service.Parser;
import by.gdev.alert.job.parser.service.category.ParsedCategory;
import by.gdev.common.model.proxy.ProxyCredentials;
import by.gdev.common.service.playwright.manager.BrowserLaunchOptions;
import by.gdev.common.service.playwright.manager.PlaywrightBrowserManager;
import by.gdev.common.service.playwright.manager.PlaywrightManagerResolver;
import com.microsoft.playwright.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Map;

@Slf4j
public abstract class PlaywrightCategoryParser implements Parser {

    @Autowired
    private PlaywrightManagerResolver managerResolver;

    @Autowired
    @Getter
    private CategoryRepository categoryRepository;

    @Value("${parser.category.retry.attempts:3}")
    private int retryAttempts;

    @Value("${parser.category.retry.delay:2000}")
    private long retryDelayMs;

    protected boolean headless;

    public ProxyCredentials getProxyWithRetry(int maxRetries, long retryDelayMs) {
        return managerResolver.getLocalManager().getProxyWithRetry(maxRetries, retryDelayMs);
    }

    protected Browser createBrowser(Playwright playwright, ProxyCredentials proxy,
                                    boolean headless, boolean isActiveProxy) {
        PlaywrightBrowserManager manager = managerResolver.getLocalManager();
        BrowserLaunchOptions options = new BrowserLaunchOptions(proxy, headless, isActiveProxy);
        return manager.createBrowser(playwright, options, getSiteName());
    }

    public void closeResources(Page page, BrowserContext context, Browser browser, Playwright playwright) {
        managerResolver.getLocalManager()
                .closeResources(page, context, browser, playwright, getSiteName());
    }

    public Playwright createPlaywright() {
        return managerResolver.getLocalManager().createPlaywright();
    }

    protected BrowserContext createBrowserContext(Browser browser, ProxyCredentials proxy, boolean useProxy) {
        PlaywrightBrowserManager manager = managerResolver.getLocalManager();
        BrowserLaunchOptions options = new BrowserLaunchOptions(proxy, headless, useProxy);
        return manager.createBrowserContext(browser, options, getSiteName());
    }

    public Map<ParsedCategory, List<ParsedCategory>> parseWithRetry(SiteSourceJob job) {
        Exception lastError = null;

        for (int attempt = 1; attempt <= retryAttempts; attempt++) {
            try {
                log.info("Попытка {}/{} парсинга категорий {}", attempt, retryAttempts, getSiteName());
                return parsePlaywright(job);
            } catch (PlaywrightException e) {
                if (e.getMessage() != null && e.getMessage().contains("Timeout")) {
                    sleepQuietly(retryDelayMs);
                    continue;
                }
                lastError = e;
                log.error("Playwright ошибка на попытке {} для {}: {}", attempt, getSiteName(), e.getMessage());
            } catch (Exception e) {
                lastError = e;
                log.error("Неожиданная ошибка на попытке {} для {}: {}", attempt, getSiteName(), e.getMessage());
            }
            sleepQuietly(retryDelayMs);
        }

        if (lastError == null) {
            log.warn("Все {} попытки парсинга категорий {} дали пустой результат",
                    retryAttempts, getSiteName());
            return Map.of();
        }

        log.error("Все {} попытки парсинга категорий {} провалились. Последняя ошибка: {}",
                retryAttempts, getSiteName(), lastError.getMessage());

        return Map.of();
    }

    private void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    protected abstract Map<ParsedCategory, List<ParsedCategory>> parsePlaywright(SiteSourceJob job);
}
