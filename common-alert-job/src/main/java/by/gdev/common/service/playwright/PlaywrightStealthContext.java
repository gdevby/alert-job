package by.gdev.common.service.playwright;

import by.gdev.common.model.SiteName;
import by.gdev.common.model.proxy.ProxyCredentials;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import io.github.kihdev.playwright.stealth4j.Stealth4j;

//Пример в https://github.com/kihdev/playwright-stealth-4j
@Component
@Slf4j
public class PlaywrightStealthContext {

    public BrowserContext createStealthBrowserContext(Browser browser, ProxyCredentials proxy, boolean useProxy, SiteName site) {
        Browser.NewContextOptions options = new Browser.NewContextOptions();
        if ("FREELANCEHUNT".equals(site)){
            options.setTimezoneId("Europe/Berlin");
        }
        BrowserContext stealthContext = Stealth4j.newStealthContext(browser);
        stealthContext.addInitScript(
                // webdriver = undefined
                "Object.defineProperty(navigator, 'webdriver', { get: () => undefined });" +

                        // window.chrome — реалистичный объект
                        "Object.defineProperty(window, 'chrome', {" +
                        "  get: () => ({" +
                        "    runtime: {}," +
                        "    app: { isInstalled: false }," +
                        "    webstore: { onInstallStageChanged: {}, onDownloadProgress: {} }" +
                        "  })" +
                        "});" +

                        // Реалистичные плагины
                        "Object.defineProperty(navigator, 'plugins', {" +
                        "  get: () => [" +
                        "    { name: 'Chrome PDF Plugin', filename: 'internal-pdf-viewer', description: 'Portable Document Format' }," +
                        "    { name: 'Chrome PDF Viewer', filename: 'mhjfbmdgcfjbbpaeojofohoefgiehjai', description: '' }," +
                        "    { name: 'Native Client', filename: 'internal-nacl-plugin', description: '' }" +
                        "  ]" +
                        "});" +

                        // Локали
                        "Object.defineProperty(navigator, 'languages', { get: () => ['en-US','ru-RU','en','ru'] });" +

                        // CPU
                        "Object.defineProperty(navigator, 'hardwareConcurrency', { get: () => 8 });" +

                        // RAM
                        "Object.defineProperty(navigator, 'deviceMemory', { get: () => 8 });"
        );
        return stealthContext;
    }

}