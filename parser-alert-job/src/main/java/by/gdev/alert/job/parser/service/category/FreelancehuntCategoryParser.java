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
public class FreelancehuntCategoryParser extends PlaywrightCategoryParser implements CategoryParser {

    private static final String JOBS_LINK = "https://freelancehunt.com/jobs";

    @Value("${freelancehunt.proxy.active}")
    private boolean freelancehuntProxyActive;

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
            ProxyCredentials proxy = freelancehuntProxyActive ? getProxyWithRetry(5, 2000) : null;
            browser = createBrowser(playwright, proxy, true, freelancehuntProxyActive);
            context = createBrowserContext(browser, proxy, freelancehuntProxyActive);
            page = context.newPage();
            page.navigate(JOBS_LINK, new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));

            Locator topItems = page.locator("ul.tree > li.tree-item");
            int topCount = topItems.count();

            for (int i = 0; i < topCount; i++) {
                Locator li = topItems.nth(i);
                Locator link = li.locator("a.tree-item-header").first();
                Locator title = link.locator("span.tree-item-title").first();

                String catName = title.count() > 0 ? title.innerText().trim() : null;
                String catValue = link.count() > 0 ? link.getAttribute("href") : "";

                if (catName == null || catName.isEmpty()) continue;
                ParsedCategory top = new ParsedCategory(null, catName, null, catValue);
                log.debug("found category {}", catName);
                List<ParsedCategory> subs = new ArrayList<>();
                Locator subLis = li.locator("ul > li");
                int subCount = subLis.count();
                for (int j = 0; j < subCount; j++) {
                    Locator subLi = subLis.nth(j);
                    Locator subLink = subLi.locator("a").first();
                    String subName = subLink.count() > 0 ? subLink.innerText().trim() : "";
                    String subValue = subLink.count() > 0 ? subLink.getAttribute("href") : "";

                    if (!subName.isEmpty()) {
                        log.debug("found subcategory {}", subName);
                        subs.add(new ParsedCategory(null, subName, null, subValue));
                    }
                }
                result.put(top, subs);
            }
        }
        finally {
            closeResources(page, context, browser ,playwright);
        }
        return result;
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.FREELANCEHUNT;
    }
}
