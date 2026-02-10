package by.gdev.alert.job.parser.service.order;

import by.gdev.alert.job.parser.domain.db.*;
import by.gdev.alert.job.parser.service.playwright.PlaywrightSiteParser;
import by.gdev.alert.job.parser.util.Pair;
import by.gdev.alert.job.parser.util.SiteName;
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

    private static final String ALL_CATEGORIES_TOKEN = "Все категории";
    private static final String TASKS_SELECTOR = "li.TasksList_listItem__2Yurg";
    private static final String CATEGORIES_SELECTOR = "ul.Categories_container__9z_KX";

    @Value("${youdo.proxy.active}")
    private boolean youdoProxyActive;

    @Value("${parser.work.youdo.com}")
    private void setActive(boolean active) {
        this.active = active;
    }

    private static boolean HEADLESS = true;

    @Value("${youdo.debug:false}")
    private void setDebug(boolean debug) {
        this.debug = debug;
    }

    @Override
    public List<OrderDTO> mapItems(String link, Long siteSourceJobId, Category category, Subcategory subCategory) {
        if (!active)
            return new ArrayList<>();
        return mapItemsWithRetry(link, youdoProxyActive, siteSourceJobId , category, subCategory);
    }

    @Override
    protected List<OrderDTO> mapItems(String link, Long siteSourceJobId, List<Pair<Category, Subcategory>> categoriesPairList){
        List<OrderDTO> orders = new ArrayList<>();
        if (!active)
            return orders;

        PlaywrightSession session = null;
        try {
            session = createSession(HEADLESS, youdoProxyActive);
            Page page = session.getPage();
            firstLoad(page);
            for(Pair<Category, Subcategory> pair: categoriesPairList){
                List<OrderDTO> categoryOrders = mapItemsWithRetry(link, youdoProxyActive, siteSourceJobId , pair, page);
                orders.addAll(categoryOrders);
            }
        }
        finally {
            closeResources(session.getPage(), session.getContext(), session.getBrowser(), session.getPlaywright());
        }
        return orders;
    }

    @Override
    protected List<OrderDTO> mapPlaywrightItems(String link, Long siteSourceJobId, Pair<Category, Subcategory> pair, Page page) {
        Category category = pair.getLeft();
        Subcategory subcategory = pair.getRight();
        // Задержка
        page.waitForTimeout(1000);
        clickCategory(page, pair.getLeft(), pair.getRight());
        // Задержка
        page.waitForTimeout(1000);
        tasksLoading(page);
        List<OrderDTO> orders = tasksParsing(page, siteSourceJobId, category, subcategory);
        clickCategory(page, ALL_CATEGORIES_TOKEN);
        // Задержка
        page.waitForTimeout(1000);
        return orders;
    }

    @Override
    public List<OrderDTO> mapPlaywrightItems(String link, Long siteSourceJobId, Category category, Subcategory subCategory) {
        List<OrderDTO> orders = new ArrayList<>();
        if (!active)
            return orders;
        PlaywrightSession session = null;
        try {
            session = createSession(HEADLESS, youdoProxyActive);
            Page page = session.getPage();
            firstLoad(page);
            clickCategory(page, category, subCategory);
            tasksLoading(page);
            orders = tasksParsing(page, siteSourceJobId, category, subCategory);
        }
        finally {
            closeResources(session.getPage(), session.getContext(), session.getBrowser(), session.getPlaywright());
        }
        return orders;
    }

    private void firstLoad(Page page){
        page.navigate(tasksUrl);
        // Ждём появления списка категорий
        page.waitForSelector(CATEGORIES_SELECTOR);
        // Сброс всех категорий
        clickCategory(page, ALL_CATEGORIES_TOKEN);
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    private void tasksLoading(Page page){
        // Ждём загрузку задач
        page.waitForSelector(TASKS_SELECTOR);
    }

    private List<OrderDTO> tasksParsing(Page page, Long siteSourceJobId, Category category, Subcategory subCategory){
        // Парсим HTML
        String html = page.content();
        Document doc = Jsoup.parse(html);

        Elements elementsOrders = doc.select(TASKS_SELECTOR);
        if (elementsOrders.isEmpty()) {
            return List.of();
        }

        List<Order> parsedOrders = elementsOrders
                .stream()
                .map(e -> parseOrder(e, siteSourceJobId, category, subCategory))
                .toList();

        return getOrdersData(parsedOrders, category, subCategory);
    }

    private void clickCategory(Page page, Category category, Subcategory subCategory){

        // Кликаем категорию
        if (subCategory != null) {
            //Если не выбрана все категории - сначала снимаем выбор кликом на Все категории
            clickCategory(page, ALL_CATEGORIES_TOKEN);
            //А затем кликаем нужную субкатегорию
            clickSubCategory(page, category.getNativeLocName(), subCategory.getNativeLocName());
        } else {
            //Если выбраны Все категории - ничего кликать не нужно
            if (!category.getNativeLocName().equals(ALL_CATEGORIES_TOKEN)){
                //Если не выбрана все категории - сначала снимаем выбор кликом на Все категории
                clickCategory(page, ALL_CATEGORIES_TOKEN);
                //А затем кликаем нужную категорию
                clickCategory(page, category.getNativeLocName());
            }
        }
    }


    /*@Override
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
            if(debug){
                browser = createBrowser(playwright, proxy, false, youdoProxyActive);
            }
            else{
                browser = createBrowser(playwright, proxy, true, youdoProxyActive);
            }
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
                //Если не выбрана все категории - сначала снимаем выбор кликом на Все категории
                clickCategory(page, ALL_CATEGORIES_TOKEN);
                //А затем кликаем нужную субкатегорию
                clickSubCategory(page, category.getNativeLocName(), subCategory.getNativeLocName());
            } else {
                //Если выбраны Все категории - ничего кликать не нужно
                if (!category.getNativeLocName().equals(ALL_CATEGORIES_TOKEN)){
                    //Если не выбрана все категории - сначала снимаем выбор кликом на Все категории
                    clickCategory(page, ALL_CATEGORIES_TOKEN);
                    //А затем кликаем нужную категорию
                    clickCategory(page, category.getNativeLocName());
                }
            }

            // Ждём загрузку задач
            page.waitForSelector(TASKS_SELECTOR);

            // Парсим HTML
            String html = page.content();
            Document doc = Jsoup.parse(html);

            Elements elementsOrders = doc.select(TASKS_SELECTOR);
            if (elementsOrders.isEmpty()) {
                return List.of();
            }

            List<Order> parsedOrders = elementsOrders
                    .stream()
                    .map(e -> parseOrder(e, siteSourceJobId, category, subCategory))
                    .toList();

            orders = getOrdersData(parsedOrders, category, subCategory);
        }
        finally {
            closeResources(page, context, browser, playwright);
        }
        return orders;
    }*/

    private void clickCategory(Page page, String categoryName) {
        // Ждём контейнер категорий
        page.waitForSelector("ul[class*='Categories_container']",
                new Page.WaitForSelectorOptions().setTimeout(15000));

        // Ищем КЛИКАБЕЛЬНЫЙ элемент категории — label
        Locator category = page.locator(
                "//label[contains(@class,'Checkbox_label')][contains(.,'" + categoryName + "')]"
        );
        Locator categoryInput = category.locator("..").locator("input[type='checkbox']");
        // Ждём, пока label станет видимым
        category.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(15000));
        boolean before = categoryInput.isChecked();
        category.click();
        page.waitForTimeout(300);
        if (before) {
            page.waitForCondition(() -> !categoryInput.isChecked());
        } else {
            page.waitForCondition(categoryInput::isChecked);
        }

        // ждём обновления списка задач
        page.waitForLoadState(LoadState.NETWORKIDLE);
        page.waitForSelector(TASKS_SELECTOR);
        if (debug){
            // ждём, пока появятся реальные задачи
            page.waitForTimeout(1000);
        }
        else {
            // ждём, пока появятся реальные задачи
            page.waitForTimeout(1000);
        }
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
            if (debug){
                page.waitForTimeout(1000); //визуальная пауза
            }
        }
        // 4. Ждём, пока список раскроется
        categoryBlock.locator("ul.Categories_subList__iasnn:not(.hidden)")
                .waitFor(new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(5000));

        if (debug){
            page.waitForTimeout(500); //как раскрывается
        }

        Locator sub = categoryBlock
                .locator("label.Checkbox_label__uNY3B")
                .filter(new Locator.FilterOptions().setHasText(subCategoryName));
        sub.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(15000));
        if (debug){
            log.error("Subcategory before click: check state {}",  sub.isChecked());
        }

        sub.scrollIntoViewIfNeeded();
        if (debug){
            page.waitForTimeout(500); //как скролит
        }
        sub.click();
        page.waitForTimeout(500);
        if (debug) {
            log.error("Subcategory after click: check state {}",  sub.isChecked());
        }
        page.waitForCondition(sub::isChecked);

        // ждём, пока React обновит DOM
        page.waitForLoadState(LoadState.NETWORKIDLE);

        if(debug) {
            // ждём, пока появятся реальные задачи
            page.waitForTimeout(5000);
        }
        else {
            //В проде секунда задержки чтобы сформировался DOM
            page.waitForTimeout(1000);
        }
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
