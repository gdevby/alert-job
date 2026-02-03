package by.gdev.alert.job.parser.service.order;

import by.gdev.alert.job.parser.domain.db.*;
import by.gdev.alert.job.parser.service.playwright.PlaywrightSiteParser;
import by.gdev.alert.job.parser.util.SiteName;
import by.gdev.alert.job.parser.util.proxy.ProxyCredentials;
import by.gdev.common.model.OrderDTO;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@DependsOn("proxyCheckerService")
public class YouDoOrderParser extends PlaywrightSiteParser {

    private final String baseUrl = "https://youdo.com";
    private final String tasksUrl = "https://youdo.com/tasks-all-opened-all";

    @Value("${youdo.proxy.active}")
    private boolean youdoProxyActive;

    @Value("${parser.work.youdo.com}")
    private void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public List<OrderDTO> mapPlaywrightItems(String link, Long siteSourceJobId, Category category, Subcategory subCategory) {
        List<OrderDTO> orders = new ArrayList<>();
        if (!active)
            return orders;
        Playwright playwright = null;
        Browser browser = null;
        BrowserContext context = null;
        Page page = null;

        try {
            playwright = createPlaywright();
            ProxyCredentials proxy = getProxyWithRetry(5, 2000);
            browser = createBrowser(playwright, proxy, youdoProxyActive);
            context = createBrowserContext(browser, null, false);

            page = context.newPage();
            //long start = System.currentTimeMillis();
            page.navigate(tasksUrl);
            //log.debug("{} загрузился за {} ms", getSiteName(), System.currentTimeMillis() - start);

            // Ждём появления списка категорий
            page.waitForSelector("ul.Categories_container__9z_KX");
            // Сброс всех категорий
            page.locator("label.Checkbox_label__uNY3B:has-text(\"Все категории\")").click();
            //page.waitForTimeout(30000);
            page.waitForLoadState(LoadState.NETWORKIDLE);

            // Кликаем категорию
            if (subCategory != null) {
                clickSubCategory(page, category.getNativeLocName(), subCategory.getNativeLocName());
            } else {
                clickCategory(page, category.getNativeLocName());
            }

            // Ждём загрузку задач
            page.waitForSelector("li.TasksList_listItem__2Yurg");

            // Парсим HTML
            String html = page.content();
            Document doc = Jsoup.parse(html);

            Elements elementsOrders = doc.select("li.TasksList_listItem__2Yurg");
            if (elementsOrders.isEmpty()) {
                return List.of();
            }

            orders = elementsOrders.stream()
                    .map(e -> parseOrder(e, siteSourceJobId, category, subCategory))
                    .filter(Objects::nonNull)
                    .filter(Order::isValidOrder)
                    /*.filter(order -> !getOrderRepository().existsByLinkCategoryAndSubCategory(
                            order.getLink(),
                            category.getId(),
                            subCategory != null ? subCategory.getId() : null
                    ))*/
                    .filter(order -> getParserService().isExistsOrder(category, subCategory, order.getLink()))
                    .map(order -> saveOrder(order, category, subCategory))
                    .toList();

        }
        finally {
            closeResources(page, context, browser, playwright);
        }
        return orders;
    }


    @Override
    public List<OrderDTO> mapItems(String link, Long siteSourceJobId, Category category, Subcategory subCategory) {
        if (!active)
            return new ArrayList<>();
        return mapItemsWithRetry(link, youdoProxyActive, siteSourceJobId , category, subCategory);
    }

    private void clickCategory(Page page, String categoryName) {
        // Ждём контейнер категорий
        page.waitForSelector("ul[class*='Categories_container']",
                new Page.WaitForSelectorOptions().setTimeout(10000));

        // Ищем КЛИКАБЕЛЬНЫЙ элемент категории — label
        Locator category = page.locator(
                "//label[contains(@class,'Checkbox_label')][contains(.,'" + categoryName + "')]"
        );
        // Ждём, пока label станет видимым
        category.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        // Кликаем по label
        category.click();
    }

    private void clickSubCategory(Page page, String categoryName, String subCategoryName) {
        // 1. Находим блок категории по тексту label
        Locator categoryBlock = page.locator(
                "//li[.//label[contains(@class,'Checkbox_label')][contains(.,'" + categoryName + "')]]"
        );
        // 2. Находим стрелку через CSS (XPath внутри locator запрещён)
        Locator arrow = categoryBlock.locator("span[class*='Categories_arrow']");
        // 3. Кликаем по стрелке (если список скрыт)
        if (categoryBlock.locator("ul.Categories_subList__iasnn.hidden").count() > 0) {
            arrow.click();
        }
        // 4. Ждём, пока список раскроется
        categoryBlock.locator("ul.Categories_subList__iasnn:not(.hidden)")
                .waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));

        Locator sub = categoryBlock
                .locator("label.Checkbox_label__uNY3B")
                .filter(new Locator.FilterOptions().setHasText(subCategoryName));
        sub.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(30000));

        sub.click();
    }

    private Order parseOrder(Element e, Long siteSourceJobId, Category category, Subcategory subCategory) {

        Element titleEl = e.selectFirst("a.TasksList_title__OaAXd");
        if (titleEl == null)
            return null;

        String link = normalizeLink(baseUrl + titleEl.attr("href"));
        Order order = getOrderRepository().findByLink(link).orElseGet(Order::new);

        order.setTitle(titleEl.text());
        order.setLink(link);

        Element descEl = e.selectFirst("div.TasksList_textBlock___jgKH");
        order.setMessage(descEl != null ? descEl.text() : "");

        Element priceEl = e.selectFirst("div.TasksList_price__m7Bqu span.nowrap");
        if (priceEl != null) {
            String priceText = priceEl.text().replace("\u00a0", " ").trim();
            String digits = priceText.replaceAll("[^0-9]", "");
            int priceValue = digits.isEmpty() ? 0 : Integer.parseInt(digits);
            order.setPrice(new Price(priceText, priceValue));
        } else {
            order.setPrice(new Price("По договоренности", 0));
        }

        order.setDateTime(new Date());

        // Источник
        ParserSource parserSource = getParserSourceRepository()
                .findBySourceAndCategoryAndSubCategory(siteSourceJobId, category.getId(),
                        subCategory != null ? subCategory.getId() : null)
                .orElseGet(() -> {
                    ParserSource ps = new ParserSource();
                    ps.setSource(siteSourceJobId);
                    ps.setCategory(category.getId());
                    ps.setSubCategory(subCategory != null ? subCategory.getId() : null);
                    return getParserSourceRepository().save(ps);
                });
        order.setSourceSite(parserSource);
        return order;
    }

    private String normalizeLink(String link) {
        int idx = link.indexOf("?searchRequestId=");
        return idx > 0 ? link.substring(0, idx) : link;
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.YOUDO;
    }
}
