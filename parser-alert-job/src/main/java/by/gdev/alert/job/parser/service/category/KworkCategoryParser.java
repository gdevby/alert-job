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
public class KworkCategoryParser extends PlaywrightCategoryParser implements CategoryParser {

    @Value("${kwork.proxy.active}")
    private boolean kworkProxyActive;

    @Value("${parser.headless.kwork.ru}")
    private void setHeadless(boolean headless) {
        this.headless = headless;
    }

    private final String KWORK_PROJECTS_LINK = "https://kwork.ru/projects";

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

            // Собираем список названий категорий
            List<String> topNames = page.locator("span.multilevel-list__label-title").allInnerTexts();

            for (String topName : topNames) {

                log.debug("TOP category: {}", topName);

                // Находим категорию по тексту
                Locator topLabel = page.locator("span.multilevel-list__label-title")
                        .filter(new Locator.FilterOptions().setHasText(topName));

                if (topLabel.count() == 0) {
                    log.warn("Категория '{}' не найдена", topName);
                    continue;
                }

                topLabel.first().click();

                // ЖДЁМ появления подкатегорий
                page.waitForSelector(
                        "ul.multilevel-list__items.multilevel-list__items--child",
                        new Page.WaitForSelectorOptions().setTimeout(5000)
                );

                // Собираем подкатегории
                List<ParsedCategory> subs = new ArrayList<>();

                Locator subLabels = page.locator(
                        "ul.multilevel-list__items.multilevel-list__items--child span.multilevel-list__label-title"
                );

                int subCount = subLabels.count();
                for (int j = 0; j < subCount; j++) {
                    String subName = subLabels.nth(j).innerText().trim();
                    log.debug("  SUB category: {}", subName);
                    subs.add(new ParsedCategory(null, subName, null, null));
                }

                result.put(new ParsedCategory(null, topName, null, null), subs);

                // Сбрасываем страницу
                page.navigate(KWORK_PROJECTS_LINK,
                        new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));
                page.waitForSelector("div.projects-filter__rubrics-list");
            }

        } finally {
            closeResources(page, context, browser, playwright);
        }

        return result;
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.KWORK;
    }
}
