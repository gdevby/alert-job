package by.gdev.common.service.playwright.manager;

import by.gdev.common.model.SiteName;
import by.gdev.common.service.playwright.human.HumanInput;
import com.microsoft.playwright.*;

import java.nio.file.Path;

public interface PlaywrightBrowserManager {

    Playwright createPlaywright();

    Browser createBrowser(Playwright playwright, BrowserLaunchOptions options, SiteName site);

    BrowserContext createBrowserContext(Browser browser, BrowserLaunchOptions options, SiteName site);

    default BrowserContext createBrowserContext(Browser browser, BrowserLaunchOptions options,
                                                SiteName site, Path storageStatePath) {
        return createBrowserContext(browser, options, site, storageStatePath, null);
    }

    default BrowserContext createBrowserContext(Browser browser, BrowserLaunchOptions options,
                                                SiteName site, Path storageStatePath,
                                                String storageStateJson) {
        return createBrowserContext(browser, options, site);
    }

    void closeResources(Page page, BrowserContext context, Browser browser,
                        Playwright playwright, SiteName site);

    default Browser.NewContextOptions baseContextOptions() {
        return new Browser.NewContextOptions()
                .setViewportSize(1366, 768)
                .setLocale("ru-RU")
                .setDeviceScaleFactor(1.0)
                .setIsMobile(false)
                .setHasTouch(false)
                .setTimezoneId("Europe/Moscow");
    }

    default void applyStealthScripts(BrowserContext context) {
        // no-op
    }

    default void humanMouse(Page page) {
        HumanInput.wander(page);
    }

    default void humanType(Page page, String selector, String text) {
        HumanInput.type(page, page.locator(selector), text);
    }

    default void humanClick(Page page, String selector) {
        HumanInput.click(page, page.locator(selector));
    }

    default void humanDelay(Page page) {
        HumanInput.pause(page);
    }

    default void humanScroll(Page page) {
        HumanInput.scroll(page);
    }
}