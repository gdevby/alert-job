package by.gdev.alert.job.parser.service.playwright;

import by.gdev.alert.job.parser.service.Parser;
import by.gdev.alert.job.parser.util.proxy.ProxyCredentials;
import com.microsoft.playwright.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public abstract class PlaywrightCategoryParser implements Parser {


    @Autowired
    private PlaywrightManager playwrightManager;

    public ProxyCredentials getProxyWithRetry(int maxRetries, long retryDelayMs) {
        return playwrightManager.getProxyWithRetry(maxRetries, retryDelayMs);
    }

    protected Playwright createPlaywright(){
        return playwrightManager.createPlaywright();
    }

    protected Browser createBrowser(Playwright playwright, ProxyCredentials proxy, boolean isActiveProxy){
        return playwrightManager.createBrowser(playwright, proxy, isActiveProxy, getSiteName());
    }

    public void closePageResources(Page page, BrowserContext context, Browser browser, Playwright playwright) {
        playwrightManager.closePageResources(page, context , browser, playwright, getSiteName());
    }

    protected BrowserContext createBrowserContext(Browser browser, ProxyCredentials proxy, boolean useProxy) {
        return playwrightManager.createBrowserContext(browser, proxy, useProxy);
    }

}
