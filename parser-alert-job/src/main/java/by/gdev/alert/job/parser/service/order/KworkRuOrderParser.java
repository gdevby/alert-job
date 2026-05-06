package by.gdev.alert.job.parser.service.order;

import by.gdev.alert.job.parser.domain.db.*;
import by.gdev.alert.job.parser.service.playwright.PlaywrightSiteParser;
import by.gdev.alert.job.parser.util.Pair;
import by.gdev.alert.job.parser.util.SiteName;
import by.gdev.alert.job.parser.util.proxy.ProxyCredentials;
import by.gdev.common.model.OrderDTO;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitUntilState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class KworkRuOrderParser extends PlaywrightSiteParser {

    @Value("${kwork.proxy.active}")
    private boolean kworkruProxyActive;

    private final String KWORK_PROJECTS_LINK = "https://kwork.ru/projects";

    @Value("${parser.work.kwork.ru}")
    private void setActive(boolean active) {
        this.active = active;
    }

    @Value("${parser.headless.kwork.ru}")
    private void setHeadless(boolean headless) {
        this.headless = headless;
    }

    @Value("${kworkru.debug:false}")
    private void setDebug(boolean debug) {
        this.debug = debug;
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.KWORK;
    }

    @Override
    protected List<OrderDTO> mapItems(String link, Long siteSourceJobId, Category category, Subcategory subCategory) {
        if (!active)
            return new ArrayList<>();
        return mapItemsWithRetry(link, kworkruProxyActive, siteSourceJobId , category, subCategory);
    }

    @Override
    protected List<OrderDTO> mapItems(String link, Long siteSourceJobId, List<Pair<Category, Subcategory>> categoriesPairList) {
        List<OrderDTO> orders = new ArrayList<>();
        if (!active)
            return orders;

        PlaywrightSession session = null;
        try {
            session = createSession(headless, kworkruProxyActive);
            Page page = session.getPage();
            page.navigate(KWORK_PROJECTS_LINK, new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));
            for(Pair<Category, Subcategory> pair: categoriesPairList){
                List<OrderDTO> categoryOrders = mapItemsWithRetry(link, kworkruProxyActive, siteSourceJobId , pair, page);
                orders.addAll(categoryOrders);
                page.navigate(KWORK_PROJECTS_LINK, new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));
            }
        }
        finally {
            closeResources(session.getPage(), session.getContext(), session.getBrowser(), session.getPlaywright());
        }
        return orders;
    }

    private List<OrderDTO> tasksParsing(Page page, Long siteSourceJobId, Category category, Subcategory subCategory) {

        // Ждём появления карточек
        page.waitForSelector("div.want-card.want-card--list",
                new Page.WaitForSelectorOptions().setTimeout(30000));

        Locator elementsOrders = page.locator("div.want-card.want-card--list");

        List<Order> parsedOrders = elementsOrders.all()
                .stream()
                .map(e -> parseOrderData(e, siteSourceJobId, category, subCategory))
                .filter(Objects::nonNull)
                .toList();

        return getOrdersData(parsedOrders, category, subCategory);
    }


    private Order parseOrderData(Locator item,
                                Long siteSourceJobId,
                                Category category,
                                Subcategory subCategory) {

        // Заголовок
        Locator titleEl = item.locator("h1.wants-card__header-title a");
        if (titleEl.count() == 0) return null;

        String link = titleEl.getAttribute("href");
        String title = titleEl.textContent().trim();

        Order order = getOrderRepository().findByLink(link).orElseGet(Order::new);
        order.setTitle(title);
        order.setMessage(title);
        order.setLink("https://kwork.ru" + link);
        order.setDateTime(new Date());

        // Цена
        parsePriceNew(order, item);

        // Источник
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


    private void parsePriceNew(Order order, Locator item) {
        Locator priceEl = item.locator(".wants-card__price div.d-inline");

        if (priceEl.count() == 0) return;

        String priceText = priceEl.innerText()
                .replace("₽", "")
                .replaceAll("[^0-9]", "")
                .trim();

        if (priceText.isEmpty()) return;

        int nominal = Integer.parseInt(priceText);
        order.setPrice(new Price(nominal + "₽", nominal));
    }


    public void clickCategory(Page page, Category category, Subcategory subCategory) {

        page.waitForSelector("div.projects-filter__rubrics-list");

        // Категория
        String categoryName = category.getNativeLocName().trim();

        Locator categoryNode = page.locator(
                "xpath=//span[contains(@class,'multilevel-list__label-title') and normalize-space(text())='" + categoryName + "']"
        );

        if (categoryNode.count() == 0) {
            log.warn("Категория '{}' не найдена", categoryName);
            return;
        }

        categoryNode.first().click();
        page.waitForTimeout(300);

        // Подкатегория
        if (subCategory != null) {

            String subName = subCategory.getNativeLocName().trim();

            Locator activeCategory = page.locator(
                    "xpath=//span[contains(@class,'multilevel-list__label') and contains(@class,'multilevel-list__label--active')]/following-sibling::ul"
            );

            Locator subNode = activeCategory.locator(
                    "xpath=.//span[contains(@class,'multilevel-list__label-title') and normalize-space(text())='" + subName + "']"
            );

            if (subNode.count() == 0) {
                log.warn("Подкатегория '{}' не найдена", subName);
            } else {
                subNode.first().click();
                page.waitForTimeout(300);
            }
        }

        page.waitForLoadState(LoadState.NETWORKIDLE);
    }


    @Override
    protected List<OrderDTO> mapPlaywrightItems(String link, Long siteSourceJobId, Pair<Category, Subcategory> pair, Page page) {
        Category category = pair.getLeft();
        Subcategory subCategory = pair.getRight();
        clickCategory(page, pair.getLeft(), pair.getRight());
        // Задержка
        page.waitForTimeout(500);
        // Задержка
        page.waitForTimeout(500);
        return tasksParsing(page, siteSourceJobId, category, subCategory);
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
            browser = createBrowser(playwright, proxy, true, kworkruProxyActive);
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

            return getOrdersData(parsedOrders, category, subCategory);
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
        order.setMessage(title);
        order.setLink(link);
        order.setDateTime(new Date());

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