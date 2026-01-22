package by.gdev.alert.job.parser.service.category;

import by.gdev.alert.job.parser.domain.db.SiteSourceJob;
import by.gdev.alert.job.parser.proxy.service.ProxyService;
import by.gdev.alert.job.parser.util.SiteName;
import by.gdev.alert.job.parser.util.proxy.ProxyCredentials;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.Proxy;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class KworkComCategoryParser implements CategoryParser {

    private Browser browser;
    private Page page;
    private final ProxyService proxyService;

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

    @Override
    public Map<ParsedCategory, List<ParsedCategory>> parse(SiteSourceJob siteSourceJob) {
        Map<ParsedCategory, List<ParsedCategory>> result = new LinkedHashMap<>();
        try {
            page = browser.newPage();
            page.navigate(siteSourceJob.getParsedURI());
            page.waitForSelector("li.js-cat-menu-thin-item");

            String html = page.content();
            Document doc = Jsoup.parse(html, siteSourceJob.getParsedURI());

            List<ParsedCategory> tops = parseTopCategories(doc);
            Elements topItems = doc.select("li.js-cat-menu-thin-item");

            for (int i = 0; i < tops.size(); i++) {
                ParsedCategory top = tops.get(i);
                Element li = topItems.get(i);
                List<ParsedCategory> subs = parseSubCategories(li);
                result.put(top, subs);
            }

        } catch (Exception e) {
            log.error("Ошибка парсинга категорий Kwork", e);
        }
        return result;
    }

    private List<ParsedCategory> parseTopCategories(Document doc) {
        List<ParsedCategory> tops = new ArrayList<>();
        Elements topItems = doc.select("li.js-cat-menu-thin-item");
        for (Element li : topItems) {
            Element topLink = li.selectFirst("a.js-category-menu-item");
            if (topLink != null) {
                tops.add(new ParsedCategory(null, topLink.text().trim(), null, topLink.attr("href").trim()));
            }
        }
        return tops;
    }

    private List<ParsedCategory> parseSubCategories(Element topLi) {
        List<ParsedCategory> subs = new ArrayList<>();
        for (Element sub : topLi.select("a.submenu-item")) {
            Element text = sub.selectFirst("span.submenu-item__text");
            String name = (text != null ? text.text() : sub.text()).trim();
            String href = sub.attr("href").trim();
            if (!name.isEmpty() && !href.isEmpty()) {
                subs.add(new ParsedCategory(null, name, null, href));
            }
        }
        return subs;
    }

    @PreDestroy
    public void shutdownBrowser() {
        if (browser != null) {
            browser.close();
        }
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.KWORKCOM;
    }
}

