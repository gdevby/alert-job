package by.gdev.alert.job.parser.service.order;

import by.gdev.alert.job.parser.domain.db.*;
import by.gdev.alert.job.parser.service.playwright.PlaywrightSiteParser;
import by.gdev.alert.job.parser.util.Pair;
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
public class WeblancerOrderParser extends PlaywrightSiteParser {

    @Value("${weblancer.proxy.active}")
    private boolean weblancerProxyActive;

    private static final boolean HEADLESS = true;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");

    @Value("${parser.work.weblancer.net}")
    private void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.WEBLANCER;
    }

    @Override
    protected List<OrderDTO> mapItems(String link, Long siteSourceJobId, Category category, Subcategory subCategory) {
        if (!active)
            return new ArrayList<>();
        return mapItemsWithRetry(link, weblancerProxyActive, siteSourceJobId, category, subCategory);
    }

    @Override
    protected List<OrderDTO> mapItems(String link, Long siteSourceJobId, List<Pair<Category, Subcategory>> categoriesPairList) {
        List<OrderDTO> orders = new ArrayList<>();
        if (!active)
            return orders;

        PlaywrightSession session = null;
        try {
            session = createSession(HEADLESS, weblancerProxyActive);
            Page page = session.getPage();

            for (Pair<Category, Subcategory> pair : categoriesPairList) {
                List<OrderDTO> categoryOrders =
                        mapItemsWithRetry(link, weblancerProxyActive, siteSourceJobId, pair, page);
                orders.addAll(categoryOrders);
            }
        } finally {
            closeResources(session.getPage(), session.getContext(), session.getBrowser(), session.getPlaywright());
        }

        return orders;
    }

    @Override
    protected List<OrderDTO> mapPlaywrightItems(String link,
                                                Long siteSourceJobId,
                                                Pair<Category, Subcategory> pair,
                                                Page page) {

        Category category = pair.getLeft();
        Subcategory subCategory = pair.getRight();

        clickCategory(page, category, subCategory);

        page.waitForTimeout(500);
        return parseOrders(page, siteSourceJobId, category, subCategory);
    }

    @Override
    protected List<OrderDTO> mapPlaywrightItems(String link,
                                                Long siteSourceJobId,
                                                Category category,
                                                Subcategory subCategory) {

        Playwright playwright = null;
        Browser browser = null;
        BrowserContext context = null;
        Page page = null;

        try {
            playwright = createPlaywright();
            ProxyCredentials proxy = weblancerProxyActive ? getProxyWithRetry(5, 2000) : null;
            browser = createBrowser(playwright, proxy, HEADLESS, weblancerProxyActive);
            context = createBrowserContext(browser, proxy, weblancerProxyActive);

            page = context.newPage();
            clickCategory(page, category, subCategory);

            return parseOrders(page, siteSourceJobId, category, subCategory);

        } finally {
            closeResources(page, context, browser, playwright);
        }
    }

    private void clickCategory(Page page, Category category, Subcategory subCategory) {
        String url = (subCategory != null) ? subCategory.getLink() : category.getLink();
        log.debug("[{}] Navigating to {}", getSiteName(), url);

        page.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));
        page.waitForTimeout(500);
    }

    private List<OrderDTO> parseOrders(Page page,
                                       Long siteSourceJobId,
                                       Category category,
                                       Subcategory subCategory) {

        page.waitForSelector("article.bg-white",
                new Page.WaitForSelectorOptions().setTimeout(30000));

        Locator items = page.locator("article.bg-white");

        List<Order> parsedOrders = items.all()
                .stream()
                .map(e -> parseOrder(e, siteSourceJobId, category, subCategory))
                .filter(Objects::nonNull)
                .toList();

        List<OrderDTO> orders = getOrdersData(parsedOrders, category, subCategory);
        orders.forEach(order ->
                log.debug("*** {} ORDER: {} , existsInDB={}", getSiteName(),
                        order.getTitle(),
                        getParserService().isExistsOrder(category, subCategory, order.getLink()))
        );
        return orders;
    }

    private Order parseOrder(Locator item,
                             Long siteSourceJobId,
                             Category category,
                             Subcategory subCategory) {

        Locator titleEl = item.locator("h2 a[href]");
        if (titleEl.count() == 0)
            return null;

        String href = titleEl.getAttribute("href");
        String link = "https://www.weblancer.net" + href;
        String title = titleEl.innerText().trim();

        Order order = getOrderRepository().findByLink(link).orElseGet(Order::new);
        order.setTitle(title);
        order.setLink(link);

        Locator descEl = item.locator("p.text-gray-600");
        order.setMessage(descEl.count() > 0 ? descEl.innerText().trim() : "");

        parseDate(order, item);
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

    private void parseDate(Order order, Locator item) {
        try {
            Locator spans = item.locator("div.flex.flex-wrap.items-center span");

            for (int i = 0; i < spans.count(); i++) {
                String text = spans.nth(i).innerText().trim();

                if (text.matches("\\d{2}\\.\\d{2}\\.\\d{4}")) {
                    order.setDateTime(dateFormat.parse(text));
                    return;
                }
            }

            log.warn("[{}] Date not found in spans", getSiteName());
        } catch (Exception e) {
            log.warn("[{}] Date parse error: {}", getSiteName(), e.getMessage());
            order.setValidOrder(false);
        }
    }

    private void parsePrice(Order order, Locator item) {
        Locator priceEl = item.locator("span.text-green-600");

        if (priceEl.count() == 0)
            return;

        String priceText = priceEl.innerText().trim();
        String numeric = priceText.replaceAll("[^0-9]", "");

        if (!numeric.isEmpty()) {
            int value = Integer.parseInt(numeric);
            order.setPrice(new Price(priceText, value));
        }
    }
}
