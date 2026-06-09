package by.gdev.alert.job.parser.service.order;

import by.gdev.alert.job.parser.domain.db.*;
import by.gdev.alert.job.parser.service.playwright.PlaywrightSiteParser;
import by.gdev.alert.job.parser.util.SiteName;
import by.gdev.common.model.OrderDTO;
import by.gdev.common.util.Pair;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.microsoft.playwright.options.WaitUntilState;
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
public class FreelanceRuOrderParser extends PlaywrightSiteParser {

    private final String baseUrl = "https://freelance.ru";
    private final String tasksUrl = "https://freelance.ru/task";

    private static final String CATEGORY_SELECTOR = "div.task-filter-group label.task-filter-check";
    private static final String TASKS_SELECTOR = ".task-feed-list .task-card";

    @Value("${freelanceru.proxy.active}")
    private boolean proxyActive;

    @Value("${parser.work.freelance.ru}")
    private void setActive(boolean active) {
        this.active = active;
    }

    @Value("${parser.headless.freelance.ru}")
    private void setHeadless(boolean headless) {
        this.headless = headless;
    }

    @Value("${freelanceru.debug:false}")
    private void setDebug(boolean debug) {
        this.debug = debug;
    }

    @Override
    public List<OrderDTO> mapItems(String link, Long siteSourceJobId, Category category, Subcategory subCategory) {
        if (!active)
            return List.of();
        return mapItemsWithRetry(link, proxyActive, siteSourceJobId, category, subCategory);
    }

    @Override
    protected List<OrderDTO> mapItems(String link, Long siteSourceJobId, List<Pair<Category, Subcategory>> categoriesPairList) {
        List<OrderDTO> orders = new ArrayList<>();
        if (!active)
            return orders;

        PlaywrightSession session = null;
        try {
            session = createSession(headless, proxyActive);
            Page page = session.getPage();
            firstLoad(page);

            for (Pair<Category, Subcategory> pair : categoriesPairList) {
                List<OrderDTO> categoryOrders =
                        mapItemsWithRetry(link, proxyActive, siteSourceJobId, pair, page);
                orders.addAll(categoryOrders);
            }

        } finally {
            if (session != null) {
                closeResources(session.getPage(), session.getContext(), session.getBrowser(), session.getPlaywright());
            }
        }
        return orders;
    }

    @Override
    protected List<OrderDTO> mapPlaywrightItems(String link, Long siteSourceJobId, Pair<Category, Subcategory> pair, Page page) {
        Category category = pair.getLeft();
        Subcategory subcategory = pair.getRight();
        page.waitForTimeout(500);
        boolean ok = clickCategoryWithRetry(page, category, true);
        if (!ok) {
            log.warn("Категория '{}' не выбрана для {}", category.getNativeLocName(), getSiteName());
            return List.of();
        }
        page.waitForTimeout(1200);

        List<OrderDTO> orders = tasksParsing(page, siteSourceJobId, category, subcategory);
        clickCategoryWithRetry(page, category, false);
        page.waitForTimeout(500);
        return orders;
    }

    @Override
    protected List<OrderDTO> mapPlaywrightItems(String link, Long siteSourceJobId, Category c, Subcategory sub) {
        return List.of();
    }

    private void firstLoad(Page page) {
        safeNavigate(page, tasksUrl);
        page.waitForSelector(CATEGORY_SELECTOR);
    }

    private void safeNavigate(Page page, String url) {
        for (int i = 1; i <= 5; i++) {
            try {
                page.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
                return;
            } catch (PlaywrightException e) {
                log.warn("Навигация не удалась {}: {}", i, e.getMessage());
                page.waitForTimeout(1500);
            }
        }
        throw new RuntimeException("Не удалось открыть страницу: " + url);
    }

    private boolean clickCategoryWithRetry(Page page, Category category, boolean enable) {
        String name = category.getNativeLocName();
        for (int i = 1; i <= 3; i++) {
            String before = page.locator("div.task-feed-list").innerHTML();
            clickCategory(page, name, enable);
            page.waitForTimeout(700);
            String after = page.locator("div.task-feed-list").innerHTML();
            if (!before.equals(after)) {
                return true;
            }
            log.warn("Категория '{}' не выбрана (попытка {})", name, i);
        }
        return false;
    }

    private void clickCategory(Page page, String categoryName, boolean enable) {
        Locator label = page.locator("label.task-filter-check").filter(new Locator.FilterOptions().setHasText(categoryName));
        //Находим чекбокс с категориями
        Locator checkbox = label.locator("input[type='checkbox']");
        // Ждём появления чекбокса
        checkbox.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000));
        boolean isChecked = checkbox.isChecked();
        // Если состояние уже такое, как нужно — ничего не делаем
        if (isChecked == enable) {
            return;
        }
        // Снимок списка ДО
        String before = page.locator("div.task-feed-list").innerHTML();
        // Кликаем по чекбоксу (элемент input)
        checkbox.click();
        // Ждём, что состояние изменилось
        if (enable) {
            page.waitForCondition(checkbox::isChecked);
        } else {
            page.waitForCondition(() -> !checkbox.isChecked());
        }
        //Ждём, что кнопка 'Применить фильтры' активна
        Locator applyBtn = page.locator("button.task-filter-apply:not(.disabled)");
        applyBtn.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000));
        //Жмём кнопку 'Применить фильтры'
        applyBtn.click();
        // Ждём обновления списка задач
        page.waitForCondition(() -> {
            String after = page.locator("div.task-feed-list").innerHTML();
            return !after.equals(before);
        });
    }

    private List<OrderDTO> tasksParsing(Page page, Long siteSourceJobId, Category category, Subcategory subCategory) {
        String html = page.content();
        Document doc = Jsoup.parse(html);

        Elements cards = doc.select(TASKS_SELECTOR);
        if (cards.isEmpty()) {
            return List.of();
        }

        List<Order> orders = cards.stream()
                .map(e -> parseOrder(e, siteSourceJobId, category, subCategory))
                .filter(Objects::nonNull)
                .toList();
        return getOrdersData(orders, category, subCategory);
    }

    private Order parseOrder(Element e, Long siteSourceJobId, Category category, Subcategory subCategory) {
        Element titleEl = e.selectFirst("a.task-card__title-link");
        if (titleEl == null)
            return null;
        String link = baseUrl + titleEl.attr("href");
        Order order = getOrderRepository().findOrdersByLink(link).stream().findFirst().orElseGet(Order::new);

        order.setTitle(titleEl.text());
        order.setLink(link);

        Element descEl = e.selectFirst("p.task-card__desc");
        order.setMessage(descEl != null ? descEl.text() : "");

        Element priceEl = e.selectFirst("div.task-card__price");
        if (priceEl != null) {
            String priceText = priceEl.text().replace("\u00a0", " ").trim();
            String digits = priceText.replaceAll("[^0-9]", "");
            int priceValue = digits.isEmpty() ? 0 : Integer.parseInt(digits);
            order.setPrice(new Price(priceText, priceValue));
        } else {
            order.setPrice(new Price("По договоренности", 0));
        }
        order.setDateTime(new Date());
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

    @Override
    public SiteName getSiteName() {
        return SiteName.FREELANCERU;
    }
}
