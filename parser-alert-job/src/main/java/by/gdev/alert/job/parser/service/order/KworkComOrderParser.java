package by.gdev.alert.job.parser.service.order;

import by.gdev.alert.job.parser.domain.db.*;
import by.gdev.alert.job.parser.service.playwright.PlaywrightSiteParser;
import by.gdev.alert.job.parser.util.SiteName;
import by.gdev.alert.job.parser.util.proxy.ProxyCredentials;
import by.gdev.common.model.OrderDTO;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class KworkComOrderParser extends PlaywrightSiteParser {

    @Value("${kworkcom.proxy.active}")
    private boolean kworkcomProxyActive;

    private final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    @Value("${parser.work.kworkcom.com}")
    private void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.KWORKCOM;
    }

    @Override
    protected List<OrderDTO> mapItems(String link, Long siteSourceJobId, Category category, Subcategory subCategory) {
        if (!active)
            return new ArrayList<>();
        return mapItemsWithRetry(link, kworkcomProxyActive, siteSourceJobId , category, subCategory);
    }

    @Override
    protected List<OrderDTO> mapPlaywrightItems(String link, Long siteSourceJobId, Category category, Subcategory subCategory) {
        Playwright playwright = null;
        Browser browser = null;
        BrowserContext context = null;
        Page page = null;

        try {
            playwright = createPlaywright();
            ProxyCredentials proxy = getProxyWithRetry(5, 2000);
            browser = createBrowser(playwright, proxy, true, kworkcomProxyActive);
            context = browser.newContext(new Browser.NewContextOptions()
                    .setJavaScriptEnabled(true)
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            );
            String url;
            if (subCategory != null){
                url = subCategory.getLink();
            }
            else {
                url = category.getLink();
            }

            page = context.newPage();
            page.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));
            page.waitForSelector("div.kwork-card-item", new Page.WaitForSelectorOptions().setTimeout(30000));

            Locator elementsOrders = page.locator("div.kwork-card-item");

            List<Order> parsedOrders = elementsOrders.all()
                    .stream()
                    .map(e -> parseOrder(e, siteSourceJobId, category, subCategory))
                    .toList();

            List<OrderDTO> orders = getOrdersData(parsedOrders, category, subCategory);
            return orders;
        }
        finally {
            closeResources(page, context, browser, playwright);
        }
    }

    private void parsePrice(Order order, Locator item) {
        Locator priceEl = item.locator(".price-wrap");

        if (priceEl.count() == 0) {
            return;
        }

        String priceText = priceEl.innerText()
                .replace("$", "")
                .replaceAll("[^0-9]", "")
                .trim();

        if (priceText.isEmpty()) {
            return;
        }
        int nominal = Integer.parseInt(priceText);
        order.setPrice(new Price("$" + nominal, nominal));
    }


    private Order parseOrder(Locator item,
                             Long siteSourceJobId,
                             Category category,
                             Subcategory subCategory) {
        Locator titleEl = item.locator(".kwork-card-item__title a");
        if (titleEl.count() == 0) return null;

        String link = titleEl.getAttribute("href");
        String title = titleEl.locator("span").textContent().trim();

        Order order = getOrderRepository().findByLink(link).orElseGet(Order::new);
        order.setTitle(title);
        order.setMessage(title); // На Kwork заголовок = краткое описание
        order.setLink(link);
        order.setDateTime(new Date()); // Дата не указана → текущая

        parsePrice(order, item);

        ParserSource parserSource = getParserSourceRepository()
                .findBySourceAndCategoryAndSubCategory(
                        siteSourceJobId,
                        category.getId(),
                        subCategory != null ? subCategory.getId() : null
                )
                .orElseGet(() -> {
                    ParserSource ps = new ParserSource();
                    ps.setSource(siteSourceJobId);
                    ps.setCategory(category.getId());
                    ps.setSubCategory(subCategory != null ? subCategory.getId() : null);
                    return getParserSourceRepository().save(ps);
                });

        order.setSourceSite(parserSource);
        order.setValidOrder(true);
        order.setOpenForAll(true);

        return order;
    }

}