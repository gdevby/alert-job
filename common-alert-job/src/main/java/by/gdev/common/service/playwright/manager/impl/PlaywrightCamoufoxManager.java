package by.gdev.common.service.playwright.manager.impl;

import by.gdev.common.model.SiteName;
import by.gdev.common.service.playwright.manager.BrowserLaunchOptions;
import by.gdev.common.service.playwright.manager.CamoufoxLauncherClient;
import by.gdev.common.service.playwright.manager.PlaywrightBrowserManager;
import com.microsoft.playwright.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PlaywrightCamoufoxManager implements PlaywrightBrowserManager {

    @Autowired
    private CamoufoxLauncherClient launcherClient;

    private final ThreadLocal<String> currentKey = new ThreadLocal<>();

    @Override
    public Playwright createPlaywright() {
        return Playwright.create();
    }

    @Override
    public Browser createBrowser(Playwright playwright, BrowserLaunchOptions options, SiteName site) {
        CamoufoxLauncherClient.LaunchResult result = launcherClient.launch(options.proxy(), site);
        currentKey.set(result.key());
        log.info("[{}] Подключение к Camoufox: {} (key={})", site, result.endpoint(), result.key());
        try {
            return playwright.firefox().connect(result.endpoint());
        } catch (Exception e) {
            launcherClient.release(result.key());
            currentKey.remove();
            throw new RuntimeException("Failed to connect to Camoufox", e);
        }
    }

    @Override
    public BrowserContext createBrowserContext(Browser browser, BrowserLaunchOptions options, SiteName site) {
        if (!browser.contexts().isEmpty()) {
            return browser.contexts().get(0);
        }
        return browser.newContext();
    }

    @Override
    public void closeResources(Page page, BrowserContext context, Browser browser,
                               Playwright playwright, SiteName site) {
        try { if (page != null && !page.isClosed()) page.close(); } catch (Exception ignored) {}
        try { if (context != null) context.close(); } catch (Exception ignored) {}
        try { if (browser != null && browser.isConnected()) browser.close(); } catch (Exception ignored) {}
        try { if (playwright != null) playwright.close(); } catch (Exception ignored) {}

        String key = currentKey.get();
        if (key != null) {
            launcherClient.release(key);
            currentKey.remove();
        }
    }
}