package by.gdev.common.service.playwright.manager.impl;

import by.gdev.common.model.SiteName;
import by.gdev.common.service.playwright.manager.BrowserLaunchOptions;
import by.gdev.common.service.playwright.manager.PlaywrightBrowserManager;
import com.microsoft.playwright.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PlaywrightCamoufoxManager implements PlaywrightBrowserManager {


    @Value("${camoufox.ws-endpoint:ws://127.0.0.1:8080/camoufox}")
    private String wsEndpoint;

    @Override
    public Playwright createPlaywright() {
        try {
            Playwright playwright = Playwright.create();
            log.debug("Playwright создан (Camoufox)");
            return playwright;
        } catch (PlaywrightException e) {
            log.error("Playwright initialization error", e);
            throw new RuntimeException("Failed to initialize Playwright", e);
        }
    }

    @Override
    public Browser createBrowser(Playwright playwright, BrowserLaunchOptions options, SiteName site) {
        return createBrowser(
                playwright,
                site
        );
    }

    @Override
    public BrowserContext createBrowserContext(Browser browser, BrowserLaunchOptions options, SiteName site) {
        return createBrowserContext(
                browser,
                site
        );
    }

    public Browser createBrowser(Playwright playwright, SiteName site) {

        log.info("[{}] Подключение к Camoufox: {}", site, wsEndpoint);

        try {
            Browser browser = playwright.firefox().connect(wsEndpoint);
            log.info("[{}] Подключение к Camoufox успешно", site);
            return browser;
        } catch (Exception e) {
            log.error("[{}] Ошибка подключения к Camoufox: {}", site, e.getMessage(), e);
            throw new RuntimeException("Failed to connect to Camoufox", e);
        }
    }

    public BrowserContext createBrowserContext(Browser browser, SiteName site) {
        try {
            if (!browser.contexts().isEmpty()) {
                log.debug("[{}] Используем существующий контекст Camoufox", site);
                return browser.contexts().get(0);
            }
            log.debug("[{}] Создаём новый контекст Camoufox", site);
            return browser.newContext();
        } catch (Exception e) {
            log.error("[{}] Ошибка создания контекста Camoufox: {}", site, e.getMessage(), e);
            throw new RuntimeException("Failed to create Camoufox context", e);
        }
    }

    public void closeResources(Page page, BrowserContext context, Browser browser, Playwright playwright, SiteName site) {
        if (page != null) {
            try { if (!page.isClosed()) page.close(); }
            catch (Exception e) { log.warn("[{}] Ошибка закрытия page: {}", site, e.getMessage()); }
        }
        if (context != null) {
            try { context.close(); }
            catch (Exception e) { log.warn("[{}] Ошибка закрытия context: {}", site, e.getMessage()); }
        }
        if (browser != null) {
            try {
                if (browser.isConnected()) {
                    browser.close();
                    log.debug("[{}] Соединение с Camoufox закрыто", site);
                }
            } catch (Exception e) { log.warn("[{}] Ошибка отключения browser: {}", site, e.getMessage()); }
        }
        if (playwright != null) {
            try { playwright.close(); }
            catch (Exception e) { log.warn("[{}] Ошибка закрытия Playwright: {}", site, e.getMessage()); }
        }
    }
}