package by.gdev.alert.job.parser.service.category;

import by.gdev.alert.job.parser.domain.db.SiteSourceJob;
import by.gdev.alert.job.parser.service.playwright.PlaywrightCategoryParser;
import by.gdev.common.model.SiteName;
import by.gdev.common.model.proxy.ProxyCredentials;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitUntilState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class KworkCategoryParser extends PlaywrightCategoryParser implements CategoryParser {

    private static final String KWORK_PROJECTS_LINK = "https://kwork.ru/projects?c=all";
    private static final String PROJECT_LINK = "https://kwork.ru/projects?c=%s";

    private static final String TOP_ITEMS_SELECTOR =
            "div.projects-filter__rubrics-list > div.multilevel-list > ul.multilevel-list__items > li";
    private static final String ACTIVE_SUBS_XPATH =
            "xpath=//div[contains(@class,'projects-filter__rubrics-list')]"
                    + "//span[contains(@class,'multilevel-list__label') and contains(@class,'multilevel-list__label--active')]"
                    + "/following-sibling::ul[1]";
    private static final String SUB_ITEMS_SELECTOR = ":scope > li.multilevel-list__item";

    @Value("${kwork.proxy.active}")
    private boolean kworkProxyActive;

    @Value("${parser.headless.kwork.ru}")
    private void setHeadless(boolean headless) {
        this.headless = headless;
    }

    @Override
    public Map<ParsedCategory, List<ParsedCategory>> parse(SiteSourceJob siteSourceJob) {
        return parseWithRetry(siteSourceJob);
    }

    @Override
    protected Map<ParsedCategory, List<ParsedCategory>> parsePlaywright(SiteSourceJob siteSourceJob) {
        Map<ParsedCategory, List<ParsedCategory>> result = new LinkedHashMap<>();

        Playwright playwright = null;
        Browser browser = null;
        BrowserContext context = null;
        Page page = null;

        try {
            playwright = createPlaywright();
            ProxyCredentials proxy = kworkProxyActive ? getProxyWithRetry(5, 2000) : null;

            browser = createBrowser(playwright, proxy, headless, kworkProxyActive);
            context = createBrowserContext(browser, proxy, kworkProxyActive);
            page = context.newPage();

            page.navigate(KWORK_PROJECTS_LINK,
                    new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));
            page.waitForSelector("div.projects-filter__rubrics-list");

            KworkCategoryLookup lookup = KworkCategoryLookup.fromPageHtml(page.content());

            Locator topItems = page.locator(TOP_ITEMS_SELECTOR);
            int topCount = topItems.count();
            log.debug("Найдено top-level категорий kwork.ru: {}", topCount);

            for (int i = 0; i < topCount; i++) {
                Locator topItem = topItems.nth(i);
                Locator topTitle = topItem.locator("span.multilevel-list__label-title").first();
                Locator topClickTarget = topItem.locator("span.multilevel-list__label").first();

                if (topTitle.count() == 0) {
                    continue;
                }

                String topName = topTitle.innerText().trim();
                if (topName.isEmpty()) {
                    continue;
                }

                log.debug("TOP category: {}", topName);
                topClickTarget.click();

                page.waitForSelector(
                        "div.projects-filter__rubrics-list span.multilevel-list__label--active",
                        new Page.WaitForSelectorOptions().setTimeout(5000)
                );

                ParsedCategory category = enrichCategory(topName, lookup);
                List<ParsedCategory> subs = parseSubcategories(page, category, lookup);
                log.debug("Kwork {} subs: {}", topName, subs.stream().map(ParsedCategory::name).toList());
                result.put(category, subs);

                page.navigate(KWORK_PROJECTS_LINK,
                        new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));
                page.waitForSelector("div.projects-filter__rubrics-list");
                topItems = page.locator(TOP_ITEMS_SELECTOR);
                topCount = topItems.count();
            }
        } finally {
            closeResources(page, context, browser, playwright);
        }

        return result;
    }

    private List<ParsedCategory> parseSubcategories(Page page, ParsedCategory category, KworkCategoryLookup lookup) {
        List<ParsedCategory> subs = new ArrayList<>();

        Locator subsList = page.locator(ACTIVE_SUBS_XPATH);
        if (subsList.count() == 0) {
            return subs;
        }

        Locator subItems = subsList.locator(SUB_ITEMS_SELECTOR);
        int subCount = subItems.count();
        String parentId = category.id() != null ? String.valueOf(category.id()) : null;

        for (int j = 0; j < subCount; j++) {
            Locator subTitle = subItems.nth(j).locator("span.multilevel-list__label-title").first();
            if (subTitle.count() == 0) {
                continue;
            }
            String subName = subTitle.innerText().trim();
            if (subName.isEmpty()) {
                continue;
            }
            log.debug("  SUB category: {}", subName);
            subs.add(enrichSubcategory(subName, parentId, lookup));
        }

        return subs;
    }

    private ParsedCategory enrichCategory(String name, KworkCategoryLookup lookup) {
        Long id = lookup.resolveTopId(name);
        if (id == null) {
            log.warn("CATID не найден для категории kwork.ru: {}", name);
            return new ParsedCategory(null, name, null, null);
        }
        return new ParsedCategory(null, name, id, String.format(PROJECT_LINK, id));
    }

    private ParsedCategory enrichSubcategory(String name, String parentId, KworkCategoryLookup lookup) {
        Long id = parentId != null ? lookup.resolveSubId(parentId, name) : null;
        if (id == null) {
            log.warn("CATID не найден для подкатегории kwork.ru: {} (parent={})", name, parentId);
            return new ParsedCategory(null, name, null, null);
        }
        return new ParsedCategory(null, name, id, String.format(PROJECT_LINK, id));
    }

    static String extractJsonObject(String body, String key) {
        String marker = "\"" + key + "\":";
        int start = body.indexOf(marker);
        if (start < 0) {
            return null;
        }

        start += marker.length();
        while (start < body.length() && Character.isWhitespace(body.charAt(start))) {
            start++;
        }
        if (start >= body.length() || body.charAt(start) != '{') {
            return null;
        }

        int depth = 0;
        for (int i = start; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return body.substring(start, i + 1);
                }
            }
        }
        return null;
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.KWORK;
    }
}
