package by.gdev.alert.job.parser.service.category;

import by.gdev.alert.job.parser.domain.db.SiteSourceJob;
import by.gdev.common.model.SiteName;
import by.gdev.alert.job.parser.service.playwright.PlaywrightCategoryParser;
import by.gdev.common.model.proxy.ProxyCredentials;
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
public class FreelanceRuCategoryParser extends PlaywrightCategoryParser implements CategoryParser {

    private static final String CATEGORY_URL = "https://freelance.ru/task";

    @Value("${freelanceru.proxy.active:false}")
    private boolean proxyActive;

    @Value("${parser.headless.freelancehunt.com}")
    private void setHeadless(boolean headless) {
        this.headless = headless;
    }

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
            ProxyCredentials proxy = proxyActive ? getProxyWithRetry(5, 2000) : null;
            browser = createBrowser(playwright, proxy, headless, proxyActive);
            context = createBrowserContext(browser, proxy, proxyActive);
            page = context.newPage();

            page.navigate(CATEGORY_URL, new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));
            page.waitForLoadState();

            // Ищем контейнер с категориями
            Locator container = page.locator("div.task-filter-group");
            if (container.count() == 0) {
                log.warn("Контейнер с категориями не найден на freelance.ru");
                return result;
            }

            // Ищем все label с категориями (плоский список чекбоксов)
            Locator labels = container.locator("label.task-filter-check");
            int count = labels.count();
            log.debug("Найдено категорий на {}: {}", getSiteName(), count);

            for (int i = 0; i < count; i++) {
                Locator label = labels.nth(i);
                Locator input = label.locator("input[type='checkbox'][name='c[]']");

                if (input.count() == 0) {
                    continue;
                }

                String value = input.getAttribute("value");
                String name = label.textContent().trim();

                if (name == null || name.isEmpty()) {
                    continue;
                }

                // Очищаем название от лишних пробелов
                String cleanName = name.replaceAll("\\s+", " ").trim();

                // Строим URL для поиска по этой категории
                String categoryUrl = "https://freelance.ru/project/search?c[]=" + value;

                ParsedCategory category = new ParsedCategory(
                        null,
                        cleanName,
                        Long.valueOf(value),
                        categoryUrl
                );

                log.debug("Найдена категория: {} (id={})", cleanName, value);
                result.put(category, Collections.emptyList());
            }

            log.debug("Успешно распаршено {} категорий с freelance.ru", result.size());

        } catch (Exception e) {
            log.error("Ошибка при парсинге категорий freelance.ru", e);
            throw new RuntimeException("Не удалось распарсить категории freelance.ru", e);
        } finally {
            closeResources(page, context, browser, playwright);
        }

        return result;
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.FREELANCERU;
    }
}