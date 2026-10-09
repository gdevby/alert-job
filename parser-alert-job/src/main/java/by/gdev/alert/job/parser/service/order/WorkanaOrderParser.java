package by.gdev.alert.job.parser.service.order;

import by.gdev.alert.job.parser.domain.db.Category;
import by.gdev.alert.job.parser.domain.db.Order;
import by.gdev.alert.job.parser.domain.db.ParserSource;
import by.gdev.alert.job.parser.domain.db.Price;
import by.gdev.alert.job.parser.domain.db.Subcategory;
import by.gdev.alert.job.parser.service.playwright.PlaywrightSiteParser;
import by.gdev.common.model.OrderDTO;
import by.gdev.common.model.SiteName;
import by.gdev.common.model.proxy.ProxyCredentials;
import by.gdev.common.util.Pair;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitUntilState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class WorkanaOrderParser extends PlaywrightSiteParser {

    private static final String BASE_URL = "https://www.workana.com";
    private static final String FILTERS_ROOT = "#block-filters";
    private static final String RESULTS = ".col-sm-12.col-md-8.search-results, .search-results";
    private static final String JOB_CARD = ".search-results .project-item";

    private final String currencyPattern = "\\b([A-Z]{3})\\b";
    private final String  = "\\d+(?:[.,]\\d+)*";

    @Value("${workana.proxy.active:true}")
    private boolean workanaProxyActive;

    @Value("${parser.work.workana.com}")
    private void setActive(boolean active) {
        this.active = active;
    }

    @Value("${parser.headless.workana.com:false}")
    private void setHeadless(boolean headless) {
        this.headless = headless;
    }

    @Value("${workana.debug:false}")
    private void setDebug(boolean debug) {
        this.debug = debug;
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.WORKANA;
    }

    @Override
    protected List<OrderDTO> mapItems(String link, Long siteSourceJobId, Category category, Subcategory subCategory) {
        if (!active) {
            return List.of();
        }
        return mapItemsWithRetry(link, workanaProxyActive, siteSourceJobId, category, subCategory);
    }

    @Override
    protected List<OrderDTO> mapItems(String link, Long siteSourceJobId, List<Pair<Category, Subcategory>> categoriesPairList) {
        if (!active) {
            return List.of();
        }
        String jobsUrl = normalizeJobsUrl(link);
        List<OrderDTO> orders = new ArrayList<>();
        PlaywrightSession session = null;
        try {
            session = createSession(headless, workanaProxyActive);
            Page page = session.getPage();
            page.navigate(jobsUrl, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            handleCookiePopup(page);
            page.waitForSelector(FILTERS_ROOT, new Page.WaitForSelectorOptions().setTimeout(60_000));

            for (Pair<Category, Subcategory> pair : categoriesPairList) {
                page.navigate(jobsUrl, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
                page.waitForSelector(FILTERS_ROOT);
                orders.addAll(mapItemsWithRetry(jobsUrl, workanaProxyActive, siteSourceJobId, pair, page));
            }
        } finally {
            if (session != null) {
                closeResources(session.getPage(), session.getContext(), session.getBrowser(), session.getPlaywright());
            }
        }
        log.info("Принято {} заказов от {}", orders.size(), getSiteName());
        return orders;
    }

    @Override
    protected List<OrderDTO> mapPlaywrightItems(String link, Long siteSourceJobId, Pair<Category, Subcategory> pair, Page page) {
        Category category = pair.getLeft();
        Subcategory subCategory = pair.getRight();
        if (!selectCategoryFilters(page, category, subCategory)) {
            log.warn("Не удалось выбрать категорию '{}' / подкатегорию '{}' на {}",
                    category.getNativeLocName(),
                    subCategory != null ? subCategory.getNativeLocName() : "—",
                    getSiteName());
            return List.of();
        }
        waitForResults(page);
        return parseOrders(page, siteSourceJobId, category, subCategory);
    }

    @Override
    protected List<OrderDTO> mapPlaywrightItems(String link, Long siteSourceJobId, Category category, Subcategory subCategory) {
        Playwright playwright = null;
        Browser browser = null;
        BrowserContext context = null;
        Page page = null;
        try {
            playwright = createPlaywright();
            ProxyCredentials proxy = workanaProxyActive ? getProxyWithRetry(5, 2000) : null;
            browser = createBrowser(playwright, proxy, headless, workanaProxyActive);
            context = createBrowserContext(browser, proxy, workanaProxyActive);
            page = context.newPage();
            String jobsUrl = normalizeJobsUrl(link);
            page.navigate(jobsUrl, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            handleCookiePopup(page);
            page.waitForSelector(FILTERS_ROOT);
            if (!selectCategoryFilters(page, category, subCategory)) {
                return List.of();
            }
            waitForResults(page);
            return parseOrders(page, siteSourceJobId, category, subCategory);
        } finally {
            closeResources(page, context, browser, playwright);
        }
    }

    private boolean selectCategoryFilters(Page page, Category category, Subcategory subCategory) {
        Optional<String> categorySlug = queryParam(category.getLink(), "category");
        if (categorySlug.isEmpty()) {
            log.warn("В ссылке категории нет параметра category: {}", category.getLink());
            return false;
        }
        String categoryInput = "#category-" + categorySlug.get();
        if (!clickFilter(page, categoryInput, "category " + category.getNativeLocName())) {
            return false;
        }
        if (subCategory == null) {
            return true;
        }
        Optional<String> subSlug = queryParam(subCategory.getLink(), "subcategory");
        if (subSlug.isEmpty()) {
            log.warn("В ссылке подкатегории нет параметра subcategory: {}", subCategory.getLink());
            return false;
        }
        String subInput = "#subcategory-" + subSlug.get();
        return clickFilter(page, subInput, "subcategory " + subCategory.getNativeLocName());
    }

    private boolean clickFilter(Page page, String inputSelector, String name) {
        Locator input = page.locator(inputSelector);
        if (input.count() == 0) {
            log.warn("Не найден фильтр {} ({})", name, inputSelector);
            return false;
        }
        String id = input.first().getAttribute("id");
        if (id == null || id.isBlank()) {
            return false;
        }
        page.locator("label[for='" + id + "']").click();
        page.waitForTimeout(getCategoryClickRetryAttemptsDelay());
        try {
            return page.locator(inputSelector).first().isChecked();
        } catch (Exception e) {
            log.debug("Фильтр {} применён (checkbox недоступен для проверки): {}", name, e.getMessage());
            return true;
        }
    }

    private void waitForResults(Page page) {
        page.waitForLoadState(LoadState.NETWORKIDLE);
        page.waitForSelector(RESULTS + " a[href*='/job/']",
                new Page.WaitForSelectorOptions().setTimeout(60_000));
        page.waitForTimeout(500);
    }

    private List<OrderDTO> parseOrders(Page page, Long siteSourceJobId, Category category, Subcategory subCategory) {
        Locator cards = page.locator(JOB_CARD);
        int count = cards.count();
        if (count == 0) {
            cards = page.locator(".search-results article");
            count = cards.count();
        }
        if (count == 0) {
            log.debug("Нет карточек заказов в {} для категории {}", getSiteName(), category.getNativeLocName());
            return List.of();
        }

        List<Order> rawOrders = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Order order = parseOrderCard(cards.nth(i), siteSourceJobId, category, subCategory);
            if (order != null) {
                rawOrders.add(order);
            }
        }
        return getOrdersData(rawOrders, category, subCategory);
    }

    private Order parseOrderCard(Locator card, Long siteSourceJobId, Category category, Subcategory subCategory) {
        Locator titleLink = card.locator("a[href*='/job/']").first();
        if (titleLink.count() == 0) {
            return null;
        }
        String href = titleLink.getAttribute("href");
        if (href == null || href.isBlank()) {
            return null;
        }
        String fullLink = href.startsWith("http") ? href : BASE_URL + href;
        String title = titleLink.innerText().trim();
        if (title.isBlank()) {
            title = titleLink.locator("span").first().getAttribute("title");
            if (title == null) {
                title = "";
            }
        }

        if (!getParserService().shouldSaveOrder(category, subCategory, fullLink)) {
            return null;
        }

        Order order = getOrderRepository().findByLink(fullLink).orElse(new Order());
        order.setTitle(title);
        order.setLink(fullLink);
        order.setDateTime(new Date());

        Locator description = card.locator(".html-text, .project-description, .description");
        if (description.count() > 0) {
            String text = description.first().innerText().trim();
            int cut = text.indexOf("Category:");
            order.setMessage(cut > 0 ? text.substring(0, cut).trim() : text);
        }

        Locator budgetEl = card.locator(".budget, .project-budget, [class*='budget']");
        if (budgetEl.count() > 0) {
            applyBudget(order, budgetEl.first().innerText().trim());
        }

        ParserSource ps = new ParserSource();
        ps.setSource(siteSourceJobId);
        ps.setCategory(category.getId());
        ps.setSubCategory(subCategory != null ? subCategory.getId() : null);
        order.setSourceSite(ps);
        order.setValidOrder(true);
        order.setOpenForAll(true);
        return order;
    }

    private void applyBudget(Order order, String budget) {
        Optional<String> extractedCurrency = extractValue(currencyPattern, budget);
        Optional<String> extractedAmount = extractValue(numberPattern, budget);
        if (extractedCurrency.isEmpty() || extractedAmount.isEmpty()) {
            order.setPrice(new Price(budget, 0));
            return;
        }
        getCurrencyRepository()
                .findByCurrencyCode(extractedCurrency.get())
                .ifPresentOrElse(currency -> {
                    double amount = convertToDouble(extractedAmount.get());
                    double converted = (amount / currency.getNominal()) * currency.getCurrencyValue();
                    order.setPrice(new Price((int) amount + " " + currency.getCurrencyCode(), (int) converted));
                }, () -> order.setPrice(new Price(budget, 0)));
    }

    private void handleCookiePopup(Page page) {
        try {
            Locator refuse = page.locator("button.ot-pc-refuse-all-handler");
            if (refuse.count() > 0) {
                refuse.first().click();
                return;
            }
            Locator prefs = page.locator("button#onetrust-pc-btn-handler");
            if (prefs.count() > 0) {
                prefs.click();
                page.waitForSelector("button.ot-pc-refuse-all-handler", new Page.WaitForSelectorOptions().setTimeout(5000));
                page.locator("button.ot-pc-refuse-all-handler").click();
            }
        } catch (Exception e) {
            log.debug("Cookie banner Workana: {}", e.getMessage());
        }
    }

    private String normalizeJobsUrl(String link) {
        if (link == null || link.isBlank()) {
            return BASE_URL + "/en/jobs?language=en";
        }
        if (link.contains("language=")) {
            return link;
        }
        return link + (link.contains("?") ? "&" : "?") + "language=en";
    }

    private Optional<String> queryParam(String url, String name) {
        if (url == null || url.isBlank()) {
            return Optional.empty();
        }
        try {
            String query = URI.create(url).getRawQuery();
            if (query == null) {
                return Optional.empty();
            }
            for (String part : query.split("&")) {
                String[] kv = part.split("=", 2);
                if (kv.length == 2 && name.equals(kv[0])) {
                    return Optional.of(URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
                }
            }
        } catch (Exception e) {
            log.debug("Не разобрать URL {}: {}", url, e.getMessage());
        }
        return Optional.empty();
    }

    private Optional<String> extractValue(String pattern, String value) {
        Matcher matcher = Pattern.compile(pattern).matcher(value);
        if (matcher.find()) {
            return Optional.of(matcher.group());
        }
        return Optional.empty();
    }

    private double convertToDouble(String value) {
        String s = value.contains(",") ? value.replace(",", "") : value.replace(".", "");
        return Double.parseDouble(s);
    }
}
