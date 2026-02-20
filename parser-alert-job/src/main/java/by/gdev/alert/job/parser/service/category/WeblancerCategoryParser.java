package by.gdev.alert.job.parser.service.category;

import by.gdev.alert.job.parser.domain.db.SiteSourceJob;
import by.gdev.alert.job.parser.service.playwright.PlaywrightCategoryParser;
import by.gdev.alert.job.parser.util.SiteName;
import by.gdev.alert.job.parser.util.proxy.ProxyCredentials;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class WeblancerCategoryParser extends PlaywrightCategoryParser implements CategoryParser {

    @Value("${weblancer.proxy.active:false}")
    private boolean weblancerProxyActive;

    @Override
    public Map<ParsedCategory, List<ParsedCategory>> parse(SiteSourceJob siteSourceJob) {
        return parseWithRetry(siteSourceJob);
    }

    @Override
    protected Map<ParsedCategory, List<ParsedCategory>> parsePlaywright(SiteSourceJob job) {
        Map<ParsedCategory, List<ParsedCategory>> result = new LinkedHashMap<>();

        Playwright playwright = null;
        Browser browser = null;
        BrowserContext context = null;
        Page page = null;

        try {
            playwright = createPlaywright();
            ProxyCredentials proxy = weblancerProxyActive ? getProxyWithRetry(5, 2000) : null;
            browser = createBrowser(playwright, proxy, true, weblancerProxyActive);
            context = createBrowserContext(browser, proxy, weblancerProxyActive);
            page = context.newPage();

            String url = "https://www.weblancer.net/tags/";
            log.debug("[{}] Navigating to {}", getSiteName(), url);

            page.navigate(url);
            page.waitForLoadState(LoadState.NETWORKIDLE);

            // Ждём появления корневых категорий
            page.waitForSelector("h2.text-xl.font-semibold.text-gray-900",
                    new Page.WaitForSelectorOptions().setTimeout(15000));

            Locator headers = page.locator("h2.text-xl.font-semibold.text-gray-900");

            for (int i = 0; i < headers.count(); i++) {
                Locator h2 = headers.nth(i);
                String title = h2.innerText().trim();
                ParsedCategory root = new ParsedCategory(null, title, null, null);
                List<ParsedCategory> subs = parseSubcategories(page, h2, job.getParsedURI());

                result.put(root, subs);
            }
        } finally {
            closeResources(page, context, browser, playwright);
        }

        return result;
    }


    private List<ParsedCategory> parseRootCategories(Page page) {
        List<ParsedCategory> roots = new ArrayList<>();

        Locator headers = page.locator("h2.text-xl.font-semibold.text-gray-900");
        int count = headers.count();

        log.debug("[WEBLANCER] Found {} root categories", count);

        for (int i = 0; i < count; i++) {
            Locator h2 = headers.nth(i);
            String title = h2.innerText().trim();

            log.debug("[WEBLANCER] Root category: {}", title);

            roots.add(new ParsedCategory(null, title, null, null));
        }

        return roots;
    }



    private List<ParsedCategory> parseSubcategories(Page page, Locator rootHeader, String baseUrl) {
        List<ParsedCategory> subs = new ArrayList<>();

        // Находим следующий div.grid после h2
        Locator container = rootHeader.locator(
                "xpath=following-sibling::div[contains(@class,'grid')][1]"
        );

        if (container.count() == 0) {
            log.warn("[WEBLANCER] No subcategory container found for {}", rootHeader.innerText().trim());
            return subs;
        }

        Locator links = container.locator("a.link-style");
        int count = links.count();

        log.debug("[WEBLANCER] Found {} subcategories for {}", count, rootHeader.innerText().trim());

        for (int i = 0; i < count; i++) {
            Locator a = links.nth(i);

            String name = a.innerText().trim();
            String href = a.getAttribute("href");

            if (href == null || href.isBlank()) {
                log.warn("[WEBLANCER] Empty href for subcategory {}", name);
                continue;
            }

            String resolved = java.net.URI.create(baseUrl).resolve(href).toString();

            log.debug("[WEBLANCER] Subcategory: {} -> {}", name, resolved);

            subs.add(new ParsedCategory(null, name, null, resolved));
        }

        return subs;
    }

    private String resolveLink(String baseUrl, String href) {
        return java.net.URI.create(baseUrl).resolve(href).toString();
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.WEBLANCER;
    }
}
