package by.gdev.alert.job.parser.service.category;

import by.gdev.alert.job.parser.domain.db.SiteSourceJob;
import by.gdev.alert.job.parser.service.playwright.PlaywrightCategoryParser;
import by.gdev.alert.job.parser.service.playwright.CaptchaService;
import by.gdev.common.model.SiteName;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class WorkspaceCategoryParser extends PlaywrightCategoryParser implements CategoryParser {

    private final CaptchaService captchaService;

    @Value("${workspace.proxy.active:false}")
    private boolean workspaceProxyActive;

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
            browser = createBrowser(playwright, null, true, workspaceProxyActive);
            context = createBrowserContext(browser, null, workspaceProxyActive);
            page = context.newPage();

            String url = job.getParsedURI();
            log.debug("{} Переход по урл {}", getSiteName(), url);

            page.navigate(url);
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            // Капча
            if (captchaService.isCaptchaPage(page)) {
                log.debug("{}  - SmartCaptcha обнаружена — пытаемся пройти...", getSiteName());
                boolean ok = captchaService.solveSmartCaptcha(page);
                if (!ok) {
                    log.warn("{} SmartCaptcha не пройдена — прерываем парсинг категорий", getSiteName());
                    return result;
                }
                log.debug("{} SmartCaptcha пройдена", getSiteName());
            }

            // Ждем отображения фильтров
            page.waitForSelector("div.vacancies__filters",
                    new Page.WaitForSelectorOptions().setTimeout(15000));

            Locator items = page.locator("div.vacancies__filters a.vacancies__filters-check-item-line");
            int count = items.count();

            log.debug("{} : Найдено {} элементов категорий", getSiteName(), count);
            for (int i = 0; i < count; i++) {
                Locator a = items.nth(i);
                String name = a.innerText().trim();
                String href = a.getAttribute("href");
                if (href == null || href.isBlank()) {
                    log.warn("{}:  Категория '{}' имеет пустую ссылку", getSiteName(), name);
                    continue;
                }

                String full = java.net.URI.create(url).resolve(href).toString();
                log.debug("{} Категория: {} -> {}", getSiteName(), name, full);

                ParsedCategory cat = new ParsedCategory(null, name, null, full);
                // Workspace.ru не имеет подкатегорий → пустой список
                result.put(cat, List.of());
            }
        } finally {
            closeResources(page, context, browser, playwright);
        }
        return result;
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.WORKSPACE;
    }
}
