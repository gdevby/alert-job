package by.gdev.alert.job.parser.service.category;

import by.gdev.alert.job.parser.domain.db.SiteSourceJob;
import by.gdev.alert.job.parser.proxy.service.ProxyService;
import by.gdev.alert.job.parser.util.SiteName;
import by.gdev.alert.job.parser.util.proxy.ProxyCredentials;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Proxy;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.microsoft.playwright.options.LoadState;

import java.util.*;


@Service
@Slf4j
@RequiredArgsConstructor
public class FreelancehuntCategoryParser implements CategoryParser {

    private final ProxyService proxyService;
    private Browser browser;

    @PostConstruct
    public void initBrowser() {
        Playwright playwright = Playwright.create();
        ProxyCredentials randomProxy = proxyService.getRandomActiveProxy();
        BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions().setHeadless(true)
                .setProxy(new Proxy("http://" + randomProxy.getHost() + ":" + randomProxy.getPort())
                        .setUsername(randomProxy.getUsername()).setPassword(randomProxy.getPassword()))
                .setArgs(Arrays.asList("--disable-blink-features=AutomationControlled", "--no-sandbox",
                        "--disable-dev-shm-usage", "--window-size=1920,1080"));
        this.browser = playwright.chromium().launch(launchOptions);
    }

    @PreDestroy
    public void shutdownBrowser() {
        if (browser != null) {
            browser.close();
        }
    }

    @Override
    public Map<ParsedCategory, List<ParsedCategory>> parse(SiteSourceJob siteSourceJob) {
        Map<ParsedCategory, List<ParsedCategory>> result = new LinkedHashMap<>();

        try (Page page = browser.newPage()) {
            page.navigate("https://freelancehunt.com/jobs");
            page.waitForLoadState(LoadState.NETWORKIDLE);

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

                List<ParsedCategory> subs = new ArrayList<>();
                Locator subLis = li.locator("ul > li");
                int subCount = subLis.count();
                for (int j = 0; j < subCount; j++) {
                    Locator subLi = subLis.nth(j);
                    Locator subLink = subLi.locator("a").first();
                    String subName = subLink.count() > 0 ? subLink.innerText().trim() : "";
                    String subValue = subLink.count() > 0 ? subLink.getAttribute("href") : "";

                    if (!subName.isEmpty()) {
                        subs.add(new ParsedCategory(null, subName, null, subValue));
                    }
                }

                result.put(top, subs);
            }

            page.close();
        } catch (Exception e) {
            log.error("Ошибка парсинга категорий Freelancehunt", e);
        }

        return result;
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.FREELANCEHUNT;
    }

}
