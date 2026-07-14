package by.gdev.alert.job.parser.service.order;

import by.gdev.alert.job.parser.domain.currency.Currency;
import by.gdev.alert.job.parser.domain.db.Category;
import by.gdev.alert.job.parser.domain.db.CurrencyEntity;
import by.gdev.alert.job.parser.domain.db.Order;
import by.gdev.alert.job.parser.domain.db.ParserSource;
import by.gdev.alert.job.parser.domain.db.Price;
import by.gdev.alert.job.parser.domain.db.Subcategory;
import by.gdev.alert.job.parser.service.playwright.PlaywrightSiteParser;
import by.gdev.alert.job.parser.util.SiteName;
import by.gdev.alert.job.parser.util.proxy.ProxyCredentials;
import by.gdev.common.model.OrderDTO;
import by.gdev.common.util.Pair;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitUntilState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
@DependsOn("proxyCheckerService")
public class FreelancehuntOrderParser extends PlaywrightSiteParser {

    private static final String JOBS_LINK = "https://freelancehunt.com/jobs";

    @Value("${freelancehunt.proxy.active}")
    private boolean freelancehuntProxyActive;

    @Value("${parser.work.freelancehunt.com}")
    private void setActive(boolean active) {
        this.active = active;
    }

    @Value("${parser.headless.freelancehunt.com}")
    private void setHeadless(boolean headless) {
        this.headless = headless;
    }

    // ========== ВЫБОР КАТЕГОРИИ ==========
    public void clickCategory(Page page, Category category) {
        String categoryName = category.getNativeLocName();
        Locator multiselect = page.locator(".multiselect");
        if (multiselect.count() == 0) {
            log.warn("Мультиселект не найден на странице FreelancehuntOrderParser");
            return;
        }
        multiselect.first().click();
        page.waitForTimeout(300);

        Locator option = page.locator(".multiselect__option, .multiselect__element")
                .filter(new Locator.FilterOptions().setHasText(categoryName));
        if (option.count() == 0) {
            log.warn("Категория '{}' не найдена в выпадающем списке", categoryName);
            return;
        }
        option.first().click();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        log.debug("Категория '{}' выбрана", categoryName);
    }

    // ========== ПАРСИНГ ОДНОГО ЗАКАЗА (новые селекторы) ==========
    private Order parseOrder(Locator item, Long siteSourceJobId, Category category, Subcategory subCategory) {
        try {
            // Заголовок и ссылка
            Locator titleEl = item.locator("h5.truncate.text-desktop-h5 > a");
            if (titleEl.count() == 0) {
                log.trace("Не найден заголовок в элементе");
                return null;
            }
            String link = titleEl.getAttribute("href");
            if (link == null || link.isEmpty()) {
                log.trace("Пустая ссылка");
                return null;
            }
            if (!link.startsWith("http")) {
                link = "https://freelancehunt.com" + link;
            }
            String title = titleEl.textContent();
            if (title == null || title.isEmpty()) {
                log.trace("Пустой заголовок");
                return null;
            }

            Order order = getOrderRepository().findByLink(link).orElseGet(Order::new);
            order.setTitle(title.trim());
            order.setLink(link);

            // Описание
            Locator descEl = item.locator("div.job-description");
            if (descEl.count() > 0) {
                String desc = descEl.textContent();
                if (desc != null) order.setMessage(desc.trim());
            }

            // Компания (необязательно)
            Locator companyEl = item.locator("div.company a.link.text-desktop-caption.text-text-secondary");
            if (companyEl.count() > 0) {
                // можно сохранить, если есть поле в Order
                // order.setCompany(companyEl.textContent());
            }

            // Дата
            Locator dateEl = item.locator("div.contents span[data-fh-tooltip]");
            if (dateEl.count() > 0) {
                String relative = dateEl.textContent();
                order.setDateTime(relative != null ? parseRelativeDate(relative.trim()) : new Date());
            } else {
                order.setDateTime(new Date());
            }

            // Цена
            Locator priceEl = item.locator("div.job-price span.text-desktop-h6");
            if (priceEl.count() > 0) {
                String priceText = priceEl.textContent();
                if (priceText != null) {
                    priceText = priceText.trim();
                    CurrencyEntity ce = getCurrencyRepository()
                            .findByCurrencyCode(priceText.contains("USD") ? Currency.USD.name() : Currency.UAH.name())
                            .orElse(null);

                    if (ce != null) {
                        String cleaned = priceText.replace("\u202F", "").replace("от ", "");
                        List<Integer> numbers = extractNumbers(cleaned);
                        if (!numbers.isEmpty()) {
                            int nominalAmount = numbers.get(0);
                            double priceValue = (nominalAmount / (double) ce.getNominal()) * ce.getCurrencyValue();
                            order.setPrice(new Price(priceText, (int) priceValue));
                        }
                    }
                }
            }

            // Источник
            ParserSource parserSource = getParserSourceRepository()
                    .findBySourceAndCategoryAndSubCategory(siteSourceJobId,
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
        } catch (Exception e) {
            log.warn("Ошибка при парсинге заказа: {}", e.getMessage());
            return null;
        }
    }

    private List<Integer> extractNumbers(String text) {
        List<Integer> numbers = new ArrayList<>();
        Matcher m = Pattern.compile("\\d+").matcher(text);
        while (m.find()) {
            numbers.add(Integer.parseInt(m.group()));
        }
        return numbers;
    }

    private Date parseRelativeDate(String text) {
        Calendar cal = Calendar.getInstance();
        try {
            String cleaned = text.toLowerCase(Locale.ROOT).trim();
            String numStr = cleaned.replaceAll("[^0-9]", " ").trim().split("\\s+")[0];
            int n = Integer.parseInt(numStr);

            if (cleaned.contains("час")) {
                cal.add(Calendar.HOUR_OF_DAY, -n);
            } else if (cleaned.contains("мин")) {
                cal.add(Calendar.MINUTE, -n);
            } else if (cleaned.contains("дн")) {
                cal.add(Calendar.DAY_OF_MONTH, -n);
            } else if (cleaned.contains("нед")) {
                cal.add(Calendar.WEEK_OF_YEAR, -n);
            } else if (cleaned.contains("месяц")) {
                cal.add(Calendar.MONTH, -n);
            } else if (cleaned.contains("год")) {
                cal.add(Calendar.YEAR, -n);
            } else {
                return new Date();
            }
            return cal.getTime();
        } catch (Exception ex) {
            log.warn("Не удалось распарсить дату: {}", text);
            return new Date();
        }
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.FREELANCEHUNT;
    }

    @Override
    public List<OrderDTO> mapItems(String link, Long siteSourceJobId, Category category, Subcategory subCategory) {
        if (!active) return new ArrayList<>();
        return mapItemsWithRetry(link, freelancehuntProxyActive, siteSourceJobId, category, subCategory);
    }

    @Override
    protected List<OrderDTO> mapItems(String link, Long siteSourceJobId, List<Pair<Category, Subcategory>> categoriesPairList) {
        List<OrderDTO> orders = new ArrayList<>();
        if (!active) return orders;

        PlaywrightSession session = null;
        try {
            session = createSession(headless, freelancehuntProxyActive);
            Page page = session.getPage();
            firstLoad(page);
            for (Pair<Category, Subcategory> pair : categoriesPairList) {
                List<OrderDTO> categoryOrders = mapItemsWithRetry(link, freelancehuntProxyActive, siteSourceJobId, pair, page);
                orders.addAll(categoryOrders);
            }
        } finally {
            closeResources(session.getPage(), session.getContext(), session.getBrowser(), session.getPlaywright());
        }
        return orders;
    }

    private void firstLoad(Page page) {
        page.navigate(JOBS_LINK, new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        log.debug("Страница загружена: {}", page.url());
    }

    private boolean tasksLoading(Page page, boolean reset) {
        page.waitForTimeout(2000);
        if (!reset && isEmptyTaskList(page)) {
            return true;
        }
        try {
            // Ждём появления хотя бы одного виджета
            page.waitForSelector("div.widget", new Page.WaitForSelectorOptions().setTimeout(10000));
            log.debug("Список задач загружен");
            return false;
        } catch (Exception e) {
            log.warn("Не дождались списка задач (таймаут 10 сек)");
            return true;
        }
    }

    // ========== ПРОВЕРКА ПУСТОГО СПИСКА ==========
    private boolean isEmptyTaskList(Page page) {
        Locator counter = page.locator("span.badge.badge--counter");
        if (counter.count() == 0) {
            return false;
        }
        String text = counter.first().textContent().trim();
        boolean empty = text.equals("0");
        log.debug("Счётчик задач: {} => пусто? {}", text, empty);
        return empty;
    }


    private void resetFilter(Page page) {
        firstLoad(page);
        tasksLoading(page, true);
    }

    private List<OrderDTO> tasksParsing(Page page, Long siteSourceJobId, Category category, Subcategory subCategory) {
        Locator items = page.locator("div.widget");
        int count = items.count();
        log.debug("Найдено виджетов для парсинга: {}", count);
        if (count == 0) {
            return List.of();
        }

        List<Order> parsedOrders = items.all().stream()
                .map(item -> parseOrder(item, siteSourceJobId, category, subCategory))
                .filter(Objects::nonNull)
                .toList();

        log.debug("Успешно распаршено заказов: {}", parsedOrders.size());
        return getOrdersData(parsedOrders, category, subCategory);
    }

    @Override
    protected List<OrderDTO> mapPlaywrightItems(String link, Long siteSourceJobId, Pair<Category, Subcategory> pair, Page page) {
        Category category = pair.getLeft();
        Subcategory subcategory = pair.getRight();

        // Выбор категории
        boolean isCategoryChanged = clickWithRetry(page, category.getNativeLocName(),
                () -> clickCategory(page, category));
        if (!isCategoryChanged) {
            log.warn("Категория {} не выбрана для сайта {}", category.getNativeLocName(), getSiteName());
            return List.of();
        }

        // Доп. ожидание после выбора
        page.waitForTimeout(1000);

        // Проверка загрузки списка
        boolean isEmptyTaskList = tasksLoading(page, false);
        page.waitForTimeout(500);

        List<OrderDTO> orders;
        if (!isEmptyTaskList) {
            orders = tasksParsing(page, siteSourceJobId, category, subcategory);
        } else {
            log.debug("Список задач для категории {} пустой", category.getNativeLocName());
            orders = List.of();
        }

        // Сброс фильтра
        page.waitForTimeout(500);
        resetFilter(page);
        return orders;
    }

    @Override
    @Deprecated
    protected List<OrderDTO> mapPlaywrightItems(String link, Long siteSourceJobId, Category category, Subcategory subCategory) {
        Playwright playwright = null;
        Browser browser = null;
        BrowserContext context = null;
        Page page = null;
        try {
            playwright = createPlaywright();
            ProxyCredentials proxy = getProxyWithRetry(5, 2000);
            browser = createBrowser(playwright, proxy, headless, freelancehuntProxyActive);
            context = createBrowserContext(browser, null, false);
            page = context.newPage();
            firstLoad(page);
            clickCategory(page, category);
            boolean isEmptyTaskList = tasksLoading(page, false);
            List<OrderDTO> orders;
            if (!isEmptyTaskList) {
                orders = tasksParsing(page, siteSourceJobId, category, subCategory);
            } else {
                orders = List.of();
                log.debug("Список задач выбранной категории {} и субкатегории {} для {} пустой", category.getNativeLocName(),
                        subCategory.getNativeLocName(), getSiteName());
            }
            resetFilter(page);
            return orders;
        } finally {
            closeResources(page, context, browser, playwright);
        }
    }
}