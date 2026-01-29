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
public class YouDoCategoryParser extends PlaywrightCategoryParser implements CategoryParser {

    private final String tasksUrl = "https://youdo.com/tasks-all-opened-all";

    @Value("${youdo.proxy.active}")
    private boolean youdoProxyActive;

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
            ProxyCredentials proxy = youdoProxyActive ? getProxyWithRetry(5, 2000) : null;
            browser = createBrowser(playwright, proxy, youdoProxyActive);
            context = createBrowserContext(browser, proxy, youdoProxyActive);
            page = context.newPage();

            page.navigate(tasksUrl);
            page.waitForLoadState(LoadState.NETWORKIDLE);
            page.waitForTimeout(10000);

            List<ParsedCategory> tops = parseTopCategories(page);

            Locator topItems = page.locator("li.Categories_item__nyDMJ");
            for (int i = 0; i < tops.size(); i++) {
                ParsedCategory top = tops.get(i);
                Locator li = topItems.nth(i);

                List<ParsedCategory> subs = parseSubCategories(li);
                result.put(top, subs);
            }
        }
        finally {
            closeResources(page, context, browser, playwright);
        }
        return result;
    }

    private List<ParsedCategory> parseTopCategories(Page page) {
        List<ParsedCategory> tops = new ArrayList<>();
        Locator topItems = page.locator("li.Categories_item__nyDMJ");

        for (int i = 0; i < topItems.count(); i++) {
            Locator li = topItems.nth(i);

            // только первый label внутри верхнего li
            Locator catLabel = li.locator("label.Checkbox_label__uNY3B").first();
            Locator catInput = li.locator("input.Checkbox_checkbox__oxdie").first();

            String catName = catLabel.count() > 0 ? catLabel.innerText().trim() : null;
            String catValue = catInput.count() > 0 ? catInput.getAttribute("value") : "";

            if (catName != null && !catName.isEmpty()) {
                log.debug("found category {}", catName);
                tops.add(new ParsedCategory(null, catName, null, catValue));
            }
        }
        return tops;
    }

    private List<ParsedCategory> parseSubCategories(Locator topLi) {
        List<ParsedCategory> subs = new ArrayList<>();
        Locator subLis = topLi.locator("li.Categories_subItem__kkuRq");

        for (int j = 0; j < subLis.count(); j++) {
            Locator subLi = subLis.nth(j);
            Locator subLabel = subLi.locator("label.Checkbox_label__uNY3B").first();
            Locator subInput = subLi.locator("input.Checkbox_checkbox__oxdie").first();

            String subName = subLabel.count() > 0 ? subLabel.innerText().trim() : "";
            String subValue = subInput.count() > 0 ? subInput.getAttribute("value") : "";

            if (!subName.isEmpty()) {
                log.debug("found subcategory {}", subName);
                subs.add(new ParsedCategory(null, subName, null, subValue));
            }
        }
        return subs;
    }


    @Override
    public SiteName getSiteName() {
        return SiteName.YOUDO;
    }
}

