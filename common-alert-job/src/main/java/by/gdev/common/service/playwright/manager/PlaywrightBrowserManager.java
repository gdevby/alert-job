package by.gdev.common.service.playwright.manager;

import by.gdev.common.model.SiteName;
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
        for (int i = 0; i < 15; i++) {
            int x = 50 + (int)(Math.random() * 300);
            int y = 50 + (int)(Math.random() * 300);
            page.mouse().move(x, y, new Mouse.MoveOptions().setSteps(5));
            page.waitForTimeout(50 + (int)(Math.random() * 120));
        }
    }

    default void humanType(Page page, String selector, String text) {
        Locator input = page.locator(selector);
        input.click();
        for (char c : text.toCharArray()) {
            page.keyboard().press(String.valueOf(c));
            page.waitForTimeout(50 + (int)(Math.random() * 100));
        }
    }

    default void humanDelay(Page page) {
        page.waitForTimeout(500 + (int)(Math.random() * 1200));
    }

    default void humanScroll(Page page) {
        for (int i = 0; i < 3; i++) {
            page.mouse().wheel(0, 200 + (int)(Math.random() * 300));
            page.waitForTimeout(200 + (int)(Math.random() * 400));
        }
    }
}