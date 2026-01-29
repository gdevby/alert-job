package by.gdev.alert.job.parser.service.playwright;

import by.gdev.alert.job.parser.proxy.service.ProxyService;
import by.gdev.alert.job.parser.util.SiteName;
import by.gdev.alert.job.parser.util.proxy.ProxyCredentials;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.Proxy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Component
public class PlaywrightManager {

    @Autowired
    private ProxyService proxyService;

    public Playwright createPlaywright() {
        Playwright playwright = null;
        try {
            playwright = Playwright.create();

        } catch (PlaywrightException e) {
            log.error("Playwright initialization error", e);
            throw new RuntimeException("Failed to initialize Playwright", e);
        } catch (Exception e) {
            log.error("Unexpected error during Playwright initialization", e);
            throw e;
        }
        return playwright;
    }

    public Browser createBrowser(Playwright playwright, ProxyCredentials proxy, boolean useProxy,  SiteName site){
        BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                .setHeadless(true)
                .setArgs(Arrays.asList(
                        "--disable-blink-features=AutomationControlled",
                        "--no-sandbox",
                        "--disable-dev-shm-usage",
                        "--window-size=1920,1080",
                        "--disable-gpu",
                        "--disable-software-rasterizer"
                ));

        ProxyCredentials usedProxy = null;
        if (useProxy) {
            usedProxy = proxy != null ? proxy : proxyService.getRandomActiveProxy();
            launchOptions.setProxy(new Proxy("http://" + usedProxy.getHost() + ":" + usedProxy.getPort())
                    .setUsername(usedProxy.getUsername())
                    .setPassword(usedProxy.getPassword()));
        }

        Browser browser = playwright.chromium().launch(launchOptions);
        if (useProxy && usedProxy != null){
            /*log.debug("Браузер для {} запущен с прокси: {}:{} (user: {})",
                    site,
                    usedProxy.getHost(),
                    usedProxy.getPort(),
                    usedProxy.getUsername());
             */
        }
        return browser;
    }

    public ProxyCredentials getProxyWithRetry(int maxRetries, long retryDelayMs) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                ProxyCredentials proxy = proxyService.getRandomActiveProxy();
                if (proxy != null) {
                    return proxy;
                }

                log.warn("Попытка {}/{}: Нет активных прокси",
                        attempt, maxRetries);

                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(retryDelayMs * attempt);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } catch (Exception e) {
                log.error("Ошибка получения прокси с попытки {}: {}",
                        attempt, e.getMessage(), e);
            }
        }

        log.warn("Ошибка получения прокси через {} попыток, продолжаем без прокси",
                maxRetries);
        return null;
    }

    public void closeResources(Page page, BrowserContext context, Browser browser, Playwright playwright, SiteName site) {
        if (page != null) {
            try {
                if (!page.isClosed()) {
                    try {
                        page.waitForLoadState(LoadState.NETWORKIDLE,
                                new Page.WaitForLoadStateOptions().setTimeout(1200));
                    } catch (Exception ignored) {}
                    page.close();
                }
            } catch (Exception e) {
                log.warn("Ошибка закрытия page в {}: {}", site, e.getMessage());
            }
        }

        if (context != null) {
            try {
                context.close();
            } catch (Exception e) {
                log.warn("Ошибка закрытия context в {}: {}", site, e.getMessage());
            }
        }

        if (browser != null) {
            try {
                if (browser.isConnected()) {
                    browser.close();
                    //log.debug("Browser закрыт для {}", site);
                }
            } catch (Exception e) {
                log.warn("Ошибка закрытия browser в {}: {}", site, e.getMessage());
            }
        }

        if (playwright != null) {
            try {
                playwright.close();
            } catch (PlaywrightException e) {
                log.warn("Playwright instance close error", e);
            } catch (Exception e) {
                log.warn("Unexpected error closing Playwright instance", e);
            } finally {
                playwright = null;
            }
        }
    }


    public BrowserContext createBrowserContext(Browser browser, ProxyCredentials proxy, boolean useProxy) {
        BrowserContext context;
        Browser.NewContextOptions options;
        if (useProxy){
            ProxyCredentials usedProxy = proxy != null ? proxy : proxyService.getRandomActiveProxy();
            options = new Browser.NewContextOptions()
                    .setViewportSize(1920, 1080)
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                            "AppleWebKit/537.36 (KHTML, like Gecko) " +
                            "Chrome/120.0.0.0 Safari/537.36")
                    .setProxy(new Proxy("http://" + usedProxy.getHost() + ":" + usedProxy.getPort())
                            .setUsername(usedProxy.getUsername())
                            .setPassword(usedProxy.getPassword()));
        }
        else {
            options = new Browser.NewContextOptions()
                    .setViewportSize(1920, 1080)
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                    "Chrome/120.0.0.0 Safari/537.36");
        }
        context = browser.newContext(options);
        context.addInitScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined});"
                + "window.chrome = { runtime: {} };"
                + "Object.defineProperty(navigator, 'plugins', {get: () => [1,2,3]});"
                + "Object.defineProperty(navigator, 'languages', {get: () => ['ru-RU','ru','en-US','en']});");
        return context;
    }
}
