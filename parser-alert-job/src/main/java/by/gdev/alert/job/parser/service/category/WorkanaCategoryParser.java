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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class WorkanaCategoryParser extends PlaywrightCategoryParser implements CategoryParser {

    private static final String DEFAULT_JOBS_URL = "https://www.workana.com/en/jobs?language=en";
    private static final String FILTERS_ROOT = "#block-filters";
    /** Дерево «Project category» в блоке фильтров */
    private static final String CATEGORY_NODES =
            FILTERS_ROOT + " ul.search-category[aria-label='Category'] li.checkbox";
    private static final String SUBCATEGORY_INPUTS =
            FILTERS_ROOT + " input[type=checkbox][id^='subcategory-']";

    @Value("${workana.proxy.active:true}")
    private boolean workanaProxyActive;

    @Value("${parser.headless.workana.com:false}")
    private void setHeadless(boolean headless) {
        this.headless = headless;
    }

    @Override
    public Map<ParsedCategory, List<ParsedCategory>> parse(SiteSourceJob siteSourceJob) {
        return parseWithRetry(siteSourceJob);
    }

    @Override
    protected Map<ParsedCategory, List<ParsedCategory>> parsePlaywright(SiteSourceJob job) {
        String jobsUrl = normalizeJobsUrl(job.getParsedURI());
        Map<ParsedCategory, List<ParsedCategory>> result = new LinkedHashMap<>();

        Playwright playwright = null;
        Browser browser = null;
        BrowserContext context = null;
        Page page = null;

        try {
            playwright = createPlaywright();
            ProxyCredentials proxy = workanaProxyActive ? getProxyWithRetry(5, 2000) : null;
            browser = createBrowser(playwright, proxy, headless, workanaProxyActive);
            context = createBrowserContext(browser, proxy, workanaProxyActive);
            page = context.newPage();

            openJobsWithFilters(page, jobsUrl);
            List<WorkanaCategoryNode> topLevel = readCategoryTree(page);
            if (topLevel.isEmpty()) {
                throw new IllegalStateException(
                        "Workana: в " + FILTERS_ROOT + " не найдено категорий (" + CATEGORY_NODES + ")");
            }

            for (WorkanaCategoryNode category : topLevel) {
                openJobsWithFilters(page, jobsUrl);
                List<ParsedCategory> subcategories = readSubcategories(page, jobsUrl, category.slug());
                ParsedCategory parsedCategory = new ParsedCategory(
                        null,
                        category.name(),
                        null,
                        buildCategoryUrl(jobsUrl, category.slug(), null));
                result.put(parsedCategory, subcategories);
                log.debug("Workana category '{}' ({}), subcategories: {}", category.name(), category.slug(),
                        subcategories.size());
            }
        } finally {
            closeResources(page, context, browser, playwright);
        }

        return result;
    }

    /**
     * Читает верхний уровень дерева категорий из {@code #block-filters} (без кликов).
     */
    List<WorkanaCategoryNode> readCategoryTree(Page page) {
        List<WorkanaCategoryNode> nodes = new ArrayList<>();
        Locator items = page.locator(CATEGORY_NODES);
        int count = items.count();
        for (int i = 0; i < count; i++) {
            Locator item = items.nth(i);
            String slug = textAttribute(item.locator("input[type=checkbox]"), "value");
            if (slug == null || slug.isBlank()) {
                continue;
            }
            String name = item.locator("label span").first().innerText().trim();
            if (name.isEmpty()) {
                continue;
            }
            nodes.add(new WorkanaCategoryNode(slug, name));
        }
        return nodes;
    }

    /**
     * После выбора категории Workana дорисовывает подкатегории в том же {@code #block-filters}.
     */
    List<ParsedCategory> readSubcategories(Page page, String jobsUrl, String categorySlug) {
        String inputId = "category-" + categorySlug;
        Locator categoryLabel = page.locator(FILTERS_ROOT + " label[for='" + inputId + "']");
        if (categoryLabel.count() == 0) {
            log.warn("Workana: не найдена категория {} ({})", categorySlug, inputId);
            return List.of();
        }
        categoryLabel.first().click();
        page.waitForTimeout(500);

        try {
            page.waitForSelector(SUBCATEGORY_INPUTS,
                    new Page.WaitForSelectorOptions().setTimeout(10_000));
        } catch (Exception e) {
            log.debug("Workana: у категории {} нет подкатегорий в фильтрах", categorySlug);
            return List.of();
        }

        List<ParsedCategory> subcategories = new ArrayList<>();
        Locator subInputs = page.locator(SUBCATEGORY_INPUTS);
        int subCount = subInputs.count();
        for (int i = 0; i < subCount; i++) {
            Locator input = subInputs.nth(i);
            String subSlug = textAttribute(input, "value");
            if (subSlug == null || subSlug.isBlank()) {
                continue;
            }
            String subInputId = textAttribute(input, "id");
            if (subInputId == null || subInputId.isBlank()) {
                continue;
            }
            Locator subLabel = page.locator(FILTERS_ROOT + " label[for='" + subInputId + "'] span");
            if (subLabel.count() == 0) {
                continue;
            }
            String subName = subLabel.first().innerText().trim();
            if (subName.isEmpty()) {
                continue;
            }
            subcategories.add(new ParsedCategory(
                    null,
                    subName,
                    null,
                    buildCategoryUrl(jobsUrl, categorySlug, subSlug)));
        }
        return subcategories;
    }

    void openJobsWithFilters(Page page, String jobsUrl) {
        page.navigate(jobsUrl, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
        handleCookiePopup(page);
        page.waitForSelector(FILTERS_ROOT, new Page.WaitForSelectorOptions().setTimeout(60_000));
        revealFiltersOnMobile(page);
        page.waitForSelector(CATEGORY_NODES, new Page.WaitForSelectorOptions().setTimeout(60_000));
    }

    private void revealFiltersOnMobile(Page page) {
        Locator toggle = page.locator(FILTERS_ROOT + " button.btn-filters");
        if (toggle.count() > 0 && toggle.first().isVisible()) {
            toggle.first().click();
            page.waitForTimeout(300);
        }
    }

    private void handleCookiePopup(Page page) {
        try {
            Locator refuse = page.locator("button.ot-pc-refuse-all-handler");
            if (refuse.count() > 0) {
                refuse.first().click();
                return;
            }
            Locator prefs = page.locator("button#onetrust-pc-btn-handler");
            if (prefs.count() > 0) {
                prefs.click();
                page.waitForSelector("button.ot-pc-refuse-all-handler",
                        new Page.WaitForSelectorOptions().setTimeout(5_000));
                page.locator("button.ot-pc-refuse-all-handler").click();
            }
        } catch (Exception e) {
            log.debug("Cookie banner Workana: {}", e.getMessage());
        }
    }

    private String normalizeJobsUrl(String link) {
        if (link == null || link.isBlank()) {
            return DEFAULT_JOBS_URL;
        }
        if (link.contains("language=")) {
            return link;
        }
        return link + (link.contains("?") ? "&" : "?") + "language=en";
    }

    private String buildCategoryUrl(String base, String categorySlug, String subcategorySlug) {
        String url = base + paramSeparator(base) + "category=" + categorySlug;
        if (subcategorySlug != null && !subcategorySlug.isBlank()) {
            url += "&subcategory=" + subcategorySlug;
        }
        return url;
    }

    private String paramSeparator(String url) {
        return url.contains("?") ? "&" : "?";
    }

    private static String textAttribute(Locator locator, String name) {
        if (locator.count() == 0) {
            return null;
        }
        return locator.first().getAttribute(name);
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.WORKANA;
    }

    private record WorkanaCategoryNode(String slug, String name) {
    }
}
