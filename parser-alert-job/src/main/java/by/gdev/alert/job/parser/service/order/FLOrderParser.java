package by.gdev.alert.job.parser.service.order;

import by.gdev.alert.job.parser.domain.db.Category;
import by.gdev.alert.job.parser.domain.db.Order;
import by.gdev.alert.job.parser.domain.db.ParserSource;
import by.gdev.alert.job.parser.domain.db.Price;
import by.gdev.alert.job.parser.domain.db.Subcategory;
import by.gdev.alert.job.parser.service.playwright.PlaywrightSiteParser;
import by.gdev.common.model.OrderDTO;
import by.gdev.common.model.SiteName;
import by.gdev.common.util.Pair;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
@DependsOn("proxyCheckerService")
public class FlRuOrderParser extends PlaywrightSiteParser {

    private static final String BASE_URL = "https://www.fl.ru";
    private static final String DEFAULT_URL = "https://www.fl.ru/projects/";

    // Карточки заказов
    private static final String TASKS_SELECTOR = "#projects-list .b-post";

    @Value("${flru.proxy.active:false}")
    private boolean proxyActive;

    @Value("${parser.work.fl.ru:true}")
    private void setActive(boolean active) {
        this.active = active;
    }

    @Value("${parser.headless.fl.ru:false}")
    private void setHeadless(boolean headless) {
        this.headless = headless;
    }

    @Value("${flru.debug:false}")
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
    protected List<OrderDTO> mapItems(String link, Long siteSourceJobId,
                                      List<Pair<Category, Subcategory>> categoriesPairList) {
        List<OrderDTO> orders = new ArrayList<>();
        if (!active)
            return orders;

        PlaywrightSession session = null;
        try {
            session = createSession(headless, proxyActive);
            Page page = session.getPage();

            for (Pair<Category, Subcategory> pair : categoriesPairList) {
                List<OrderDTO> categoryOrders =
                        mapItemsWithRetry(link, proxyActive, siteSourceJobId, pair, page);
                orders.addAll(categoryOrders);
            }
        } finally {
            if (session != null) {
                closeResources(session.getPage(), session.getContext(),
                        session.getBrowser(), session.getPlaywright());
            }
        }
        return orders;
    }

    @Override
    protected List<OrderDTO> mapPlaywrightItems(String link, Long siteSourceJobId,
                                                Pair<Category, Subcategory> pair, Page page) {
        Category category = pair.getLeft();
        Subcategory subcategory = pair.getRight();

        String targetUrl = buildCategoryUrl(link, category, subcategory);
        log.info("FL.ru: переход на {}", targetUrl);

        safeNavigate(page, targetUrl);
        page.waitForTimeout(1200);

        // Ждём появления карточек заказов
        try {
            page.waitForSelector(TASKS_SELECTOR,
                    new Page.WaitForSelectorOptions().setTimeout(15000));
        } catch (Exception e) {
            log.warn("FL.ru: карточки заказов не найдены на {}: {}", targetUrl, e.getMessage());
            return List.of();
        }

        return tasksParsing(page, siteSourceJobId, category, subcategory);
    }

    @Override
    protected List<OrderDTO> mapPlaywrightItems(String link, Long siteSourceJobId,
                                                Category c, Subcategory sub) {
        return List.of();
    }

    private String buildCategoryUrl(String baseLink, Category category, Subcategory subcategory) {
        String base = (baseLink != null && !baseLink.isBlank()) ? baseLink : DEFAULT_URL;
        // Убираем trailing slash
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        // Если уже содержит /projects/category — считаем, что link готов
        if (base.contains("/projects/category/")) {
            return base + "/";
        }
        // Иначе достраиваем по slug категории/подкатегории, если они у тебя есть
        // (замени getSlug() на то поле, что реально хранит URL-slug в твоей Category/Subcategory)
        StringBuilder sb = new StringBuilder(base).append("/category/");
        sb.append(category != null ? category.getNativeLocName() : "");
        if (subcategory != null) {
            sb.append("/").append(subcategory.getNativeLocName());
        }
        sb.append("/");
        return sb.toString();
    }

    private void safeNavigate(Page page, String url) {
        for (int i = 1; i <= 5; i++) {
            try {
                page.navigate(url,
                        new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
                return;
            } catch (PlaywrightException e) {
                log.warn("Навигация не удалась ({}): {}", i, e.getMessage());
                page.waitForTimeout(1500);
            }
        }
        throw new RuntimeException("Не удалось открыть страницу: " + url);
    }

    private List<OrderDTO> tasksParsing(Page page, Long siteSourceJobId,
                                        Category category, Subcategory subCategory) {
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

    private Order parseOrder(Element e, Long siteSourceJobId,
                             Category category, Subcategory subCategory) {
        // Заголовок и ссылка
        Element titleEl = e.selectFirst("h2.b-post__title > a");
        if (titleEl == null) {
            return null;
        }
        String href = titleEl.attr("href");
        String link = href.startsWith("http") ? href : BASE_URL + href;

        Order order = getOrderRepository().findOrdersByLink(link).stream()
                .findFirst()
                .orElseGet(Order::new);

        order.setTitle(titleEl.text());
        order.setLink(link);

        // Описание
        Element descEl = e.selectFirst("div.b-post__body .b-post__txt");
        order.setMessage(descEl != null ? descEl.text() : "");

        // Цена
        Element priceEl = e.selectFirst("div.b-post__price span");
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
                .findBySourceAndCategoryAndSubCategory(
                        siteSourceJobId,
                        category.getId(),
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
        return SiteName.FLRU;
    }
}