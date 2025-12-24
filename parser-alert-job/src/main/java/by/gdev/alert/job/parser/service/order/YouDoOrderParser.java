package by.gdev.alert.job.parser.service.order;

import by.gdev.alert.job.parser.domain.db.*;
import by.gdev.alert.job.parser.repository.OrderRepository;
import by.gdev.alert.job.parser.repository.ParserSourceRepository;
import by.gdev.alert.job.parser.service.ParserService;
import by.gdev.alert.job.parser.util.SiteName;
import by.gdev.common.model.OrderDTO;
import by.gdev.common.model.SourceSiteDTO;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitForSelectorState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class YouDoOrderParser extends AbsctractSiteParser {

    private final ParserService service;
    private final OrderRepository orderRepository;
    private final ParserSourceRepository parserSourceRepository;
    private final ModelMapper mapper;

    private final String baseUrl = "https://youdo.com";
    private final String tasksUrl = "https://youdo.com/tasks-all-opened-all";

    @Override
    public List<OrderDTO> mapItems(String link, Long siteSourceJobId, Category category, Subcategory subCategory) {

        try (Playwright playwright = Playwright.create()) {

            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions()
                            .setHeadless(true) // Запуск браузера БЕЗ окна (невидимый режим)
                            .setArgs(List.of(
                                    "--headless=new", // Новый headless‑движок Chrome (перекрывает setHeadless)
                                    "--use-gl=swiftshader", // Использовать программный рендеринг (без GPU)
                                    "--disable-gpu", // Полностью отключить GPU (важно для серверов/докера)
                                    "--disable-dev-shm-usage", // Не использовать /dev/shm (в докере мало памяти)
                                    "--no-sandbox", // Отключить sandbox (обязательно в Docker/CI)
                                    "--disable-blink-features=AutomationControlled", // Скрыть факт автоматизации (anti‑bot)
                                    "--disable-infobars" // Убрать баннер "Chrome is being controlled by automated test software"
                            ))
            );

            BrowserContext context = browser.newContext(
                    new Browser.NewContextOptions()
                            .setViewportSize(1920, 1080) // Эмулируем размер экрана браузера (виртуальный монитор 1920×1080)
                            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                    "Chrome/120.0.0.0 Safari/537.36") // Подменяем User-Agent, чтобы выглядеть как обычный Chrome
            );

            context.addInitScript(
                    "Object.defineProperty(navigator, 'webdriver', { get: () => undefined })"
            ); // Убираем признак автоматизации: navigator.webdriver → undefined (антибот‑маскировка)


            Page page = context.newPage();
            page.navigate(tasksUrl);
            //log.info("PAGE HTML:\n{}", page.content());

            // Ждём появления списка категорий
            page.waitForSelector("ul.Categories_container__9z_KX");
            // Сброс всех категорий
            page.locator("label.Checkbox_label__uNY3B:has-text(\"Все категории\")").click();
            page.waitForTimeout(300);

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
            System.out.println("Found YouDo order elements: " + elementsOrders.size());
            if (elementsOrders.isEmpty()) {
                log.warn("YouDo: no order elements found");
                return List.of();
            }

            List<OrderDTO> orders = elementsOrders.stream()
                    .map(e -> parseOrder(e, siteSourceJobId, category, subCategory))
                    .filter(Objects::nonNull)
                    .filter(Order::isValidOrder)
                    .filter(order -> !orderRepository.existsByLink(order.getLink()))
                    .map(order -> saveOrder(order, category, subCategory))
                    .toList();

            browser.close();
            return orders;

        } catch (Exception e) {
            log.error("Playwright error: YouDoOrderParser", e);
            return List.of();
        }
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
                .setTimeout(5000));

        sub.click();
    }

    private Order parseOrder(Element e, Long siteSourceJobId, Category category, Subcategory subCategory) {

        Element titleEl = e.selectFirst("a.TasksList_title__OaAXd");
        if (titleEl == null)
            return null;

        String link = normalizeLink(baseUrl + titleEl.attr("href"));
        Order order = orderRepository.findByLink(link).orElseGet(Order::new);

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
        ParserSource parserSource = parserSourceRepository
                .findBySourceAndCategoryAndSubCategory(siteSourceJobId, category.getId(),
                        subCategory != null ? subCategory.getId() : null)
                .orElseGet(() -> {
                    ParserSource ps = new ParserSource();
                    ps.setSource(siteSourceJobId);
                    ps.setCategory(category.getId());
                    ps.setSubCategory(subCategory != null ? subCategory.getId() : null);
                    return parserSourceRepository.save(ps);
                });
        order.setSourceSite(parserSource);
        return order;
    }

    private OrderDTO saveOrder(Order e, Category category, Subcategory subCategory) {
        service.saveOrderLinks(category, subCategory, e.getLink());

        ParserSource ps = e.getSourceSite();
        ParserSource existing = parserSourceRepository
                .findBySourceAndCategoryAndSubCategory(
                        ps.getSource(),
                        ps.getCategory(),
                        ps.getSubCategory()
                )
                .orElseGet(() -> parserSourceRepository.save(ps));

        e.setSourceSite(existing);
        e = orderRepository.save(e);

        OrderDTO dto = mapper.map(e, OrderDTO.class);
        SourceSiteDTO source = dto.getSourceSite();
        source.setCategoryName(category.getNativeLocName());
        if (subCategory != null)
            source.setSubCategoryName(subCategory.getNativeLocName());
        dto.setSourceSite(source);

        return dto;
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
