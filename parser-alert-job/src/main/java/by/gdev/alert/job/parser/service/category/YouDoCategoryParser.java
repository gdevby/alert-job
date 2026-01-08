package by.gdev.alert.job.parser.service.category;

import by.gdev.alert.job.parser.domain.db.SiteSourceJob;
import by.gdev.alert.job.parser.util.SiteName;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class YouDoCategoryParser implements CategoryParser {

    private Playwright playwright;
    private Browser browser;

    private final String tasksUrl = "https://youdo.com/tasks-all-opened-all";


    @PostConstruct
    public void initBrowser() {
        playwright = Playwright.create();
        this.browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions()
                        .setHeadless(true) // Запуск браузера БЕЗ окна (невидимый режим)
                        .setArgs(List.of(
                                "--headless=new", // Новый headless‑движок Chrome (перекрывает setHeadless)
                                "--use-gl=swiftshader", // Использовать программный рендеринг (без GPU)
                                "--disable-gpu", // Полностью отключить GPU (важно для серверов/докера)
                                "--disable-dev-shm-usage", // Не использовать /dev/shm (в докере мало памяти)
                                "--no-sandbox", // Отключить sandbox (обязательно в Docker/CI)
                                "--disable-blink-features=AutomationControlled", // Скрыть факт автоматизации (anti‑bot)
                                "--disable-infobars" // Убрать баннер "Chrome is being controlled by automated test software"
                        ))
        );
    }

    @PreDestroy
    public void shutdownBrowser() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }

    @Override
    public Map<ParsedCategory, List<ParsedCategory>> parse(SiteSourceJob siteSourceJob) {
        Map<ParsedCategory, List<ParsedCategory>> result = new LinkedHashMap<>();

        try (BrowserContext context = browser.newContext(
                new Browser.NewContextOptions()
                        .setViewportSize(1920, 1080)
                        .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                "Chrome/120.0.0.0 Safari/537.36")
        )) {
            context.addInitScript("Object.defineProperty(navigator, 'webdriver', { get: () => undefined })");
            Page page = context.newPage();

            page.navigate(tasksUrl);
            page.waitForLoadState(LoadState.NETWORKIDLE);
            page.waitForTimeout(1000);

            List<ParsedCategory> tops = parseTopCategories(page);

            Locator topItems = page.locator("li.Categories_item__nyDMJ");
            for (int i = 0; i < tops.size(); i++) {
                ParsedCategory top = tops.get(i);
                Locator li = topItems.nth(i);

                List<ParsedCategory> subs = parseSubCategories(li);
                result.put(top, subs);
            }

            page.close();
            context.close();
        } catch (Exception e) {
            log.error("Ошибка парсинга категорий YouDo", e);
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

