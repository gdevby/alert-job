package by.gdev.alert.job.parser.service.category;

import by.gdev.alert.job.parser.domain.db.SiteSourceJob;
import by.gdev.alert.job.parser.service.playwright.PlaywrightCategoryParser;
import by.gdev.alert.job.parser.util.SiteName;
import by.gdev.alert.job.parser.util.proxy.ProxyCredentials;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
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
            ProxyCredentials proxy = freelancehuntProxyActive ? getProxyWithRetry(5, 2000) : null;
            browser = createBrowser(playwright, proxy, headless, freelancehuntProxyActive);
            context = createBrowserContext(browser, proxy, freelancehuntProxyActive);
            page = context.newPage();

            // Загрузка страницы
            page.navigate(JOBS_LINK, new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);

            // Обработка ошибок (403, Cloudflare)
            if (isSecurityCheckPage(page) || isError403(page)) {
                log.warn("Обнаружена ошибка или проверка безопасности, пробуем восстановиться");
                handle403IfPresent(page);
                page.waitForTimeout(2000);
            }

            // Поиск мультиселекта
            Locator multiselect = page.locator(".multiselect");
            if (multiselect.count() == 0) {
                log.warn("Мультиселект не найден на странице, парсинг категорий невозможен");
                return result;
            }

            // Клик по мультиселекту для открытия списка
            multiselect.first().click();
            page.waitForTimeout(300);

            // Сбор опций
            Locator options = page.locator(".multiselect__option, .multiselect__element");
            int count = options.count();
            log.debug("Найдено опций в мультиселекте: {}", count);

            if (count == 0) {
                log.warn("Не найдено опций в мультиселекте");
                return result;
            }

            // Проходим по всем опциям
            for (int i = 0; i < count; i++) {
                Locator opt = options.nth(i);
                // Проверяем, что опция видима (не скрыта)
                if (!opt.isVisible()) continue;

                String text = opt.innerText().trim();
                if (text.isEmpty()) continue;

                // Приводим к нижнему регистру для удобства сравнения
                String lower = text.toLowerCase();

                // Пропускаем служебные / некатегорийные варианты
                if (lower.equals("все") || lower.equals("любая") || lower.equals("any") ||
                        lower.contains("list is empty") || lower.contains("ничего не найдено") ||
                        lower.contains("empty") || lower.contains("ничего") ||
                        lower.contains("no results") || lower.contains("нет результатов")) {
                    log.trace("Пропускаем служебную опцию: {}", text);
                    continue;
                }

                // Дополнительно можно проверить наличие атрибута value (если он есть)
                // String value = opt.getAttribute("data-value");
                // if (value == null || value.isEmpty()) continue;

                ParsedCategory category = new ParsedCategory(null, text, null, null);
                log.debug("Найдена категория: {}", text);
                result.put(category, Collections.emptyList()); // подкатегорий нет
            }

            if (result.isEmpty()) {
                log.warn("Не удалось найти ни одной категории (возможно, все опции были служебными)");
            }

        } catch (Exception e) {
            log.error("Ошибка при парсинге категорий: {}", e.getMessage(), e);
        } finally {
            closeResources(page, context, browser, playwright);
        }
        return result;
    }

    // --- Вспомогательные методы (аналогичны парсеру заказов) ---

    private boolean isError403(Page page) {
        try {
            return page.locator("text=Request failed with status code 403").count() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isSecurityCheckPage(Page page) {
        try {
            return page.locator("text=Этот веб-сайт использует сервис безопасности").count() > 0
                    || (page.title() != null && page.title().contains("Один момент…"))
                    || page.locator("#ncOB5").count() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void handle403IfPresent(Page page) {
        try {
            Locator retryButton = page.locator("button.button--secondary:has-text('Повторить')");
            if (retryButton.count() > 0 && retryButton.isVisible()) {
                retryButton.click();
                log.debug("Кнопка 'Повторить' нажата для страницы категорий");
                page.waitForTimeout(2000);
            }
        } catch (Exception e) {
            log.warn("Не удалось обработать 403 на странице категорий: {}", e.getMessage());
        }
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.FREELANCEHUNT;
    }
}