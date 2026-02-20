package by.gdev.alert.job.parser.service.category;

import by.gdev.alert.job.parser.domain.db.SiteSourceJob;
import by.gdev.alert.job.parser.service.playwright.PlaywrightCategoryParser;
import by.gdev.alert.job.parser.util.SiteName;
import by.gdev.alert.job.parser.util.proxy.ProxyCredentials;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class FreelancerCategoryParser extends PlaywrightCategoryParser implements CategoryParser {

    private static final String CATEGORIES_URL = "https://www.freelancer.com/job/";

    @Value("${freelancer.proxy.active:false}")
    private boolean freelancerProxyActive;

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
            ProxyCredentials proxy = freelancerProxyActive ? getProxyWithRetry(5, 2000) : null;
            browser = createBrowser(playwright, proxy, true, freelancerProxyActive);
            context = createBrowserContext(browser, proxy, freelancerProxyActive);
            page = context.newPage();

            page.navigate(CATEGORIES_URL,
                    new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));

            page.waitForTimeout(1500);

            Locator titles = page.locator("h3.PageJob-category-title");
            int titleCount = titles.count();
            log.debug("Found {} top level categories: {} for site", titleCount, getSiteName());

            for (int i = 0; i < titleCount; i++) {
                Locator titleEl = titles.nth(i);

                String catName = titleEl.innerText().trim();
                catName = catName.replaceAll("\\(.*?\\)", "").trim();

                ParsedCategory top = new ParsedCategory(catName, catName, null, null);
                log.debug("{}: category {}", getSiteName(), catName);

                List<ParsedCategory> subs = new ArrayList<>();

                Locator ul = titleEl.locator("xpath=following::ul[contains(@class,'PageJob-browse-list')][1]");

                if (ul.count() > 0) {
                    Locator subLinks = ul.locator("li a.PageJob-category-link");
                    int subCount = subLinks.count();

                    for (int j = 0; j < subCount; j++) {
                        Locator sub = subLinks.nth(j);
                        String subName = sub.innerText().trim();
                        subName = subName.replaceAll("\\(.*?\\)", "").trim();
                        String subHref = sub.getAttribute("href");
                        log.debug("Found subcategory {} for category {} in site {}", subName, top.translatedName(), getSiteName());
                        subs.add(new ParsedCategory(null, subName, null, subHref));
                    }
                }
                result.put(top, subs);
            }

        } finally {
            closeResources(page, context, browser, playwright);
        }

        return result;
    }



    @Override
    public SiteName getSiteName() {
        return SiteName.FREELANCER;
    }
}

