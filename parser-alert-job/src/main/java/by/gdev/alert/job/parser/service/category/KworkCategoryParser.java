package by.gdev.alert.job.parser.service.category;

import by.gdev.alert.job.parser.domain.db.SiteSourceJob;
import by.gdev.alert.job.parser.service.playwright.PlaywrightCategoryParser;
import by.gdev.alert.job.parser.util.SiteName;
import by.gdev.alert.job.parser.util.proxy.ProxyCredentials;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class KworkCategoryParser extends PlaywrightCategoryParser implements CategoryParser {

    @Value("${kwork.proxy.active}")
    private boolean kworkProxyActive;

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
            browser = createBrowser(playwright, proxy, kworkProxyActive);
            context = createBrowserContext(browser, proxy, kworkProxyActive);
            page = context.newPage();
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
        }
        finally {
            closeResources(page, context, browser, playwright);
        }
        return result;
    }

    private List<ParsedCategory> parseTopCategories(Document doc) {
        List<ParsedCategory> tops = new ArrayList<>();
        Elements topItems = doc.select("li.js-cat-menu-thin-item");
        for (Element li : topItems) {
            Element topLink = li.selectFirst("a.js-category-menu-item");
            if (topLink != null) {
                log.debug("found category {}", topLink.text().trim());
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
                log.debug("found subcategory {}", name);
                subs.add(new ParsedCategory(null, name, null, href));
            }
        }
        return subs;
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.KWORK;
    }
}

