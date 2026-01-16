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
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class YouDoOrderParser extends AbsctractSiteParser {

    private final ParserService service;
    private final OrderRepository orderRepository;
    private final ParserSourceRepository parserSourceRepository;
    private final ModelMapper mapper;
    private final PlatformTransactionManager transactionManager;
    private Browser browser;

    private final String baseUrl = "https://youdo.com";
    private final String tasksUrl = "https://youdo.com/tasks-all-opened-all";

    @Value("${parser.work.youdo.com}")
    private boolean active;

    @Value("${parser.youdo.batch-size:5}")
    private int batchSize;

    @Value("${parser.youdo.transaction-timeout:300}")
    private int transactionTimeout;

    private TransactionTemplate transactionTemplate;

    @PostConstruct
    public void init() {
        Playwright playwright = Playwright.create();
        this.browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions()
                        .setHeadless(true)
                        .setArgs(List.of(
                                "--headless=new",
                                "--use-gl=swiftshader",
                                "--disable-gpu",
                                "--disable-dev-shm-usage",
                                "--no-sandbox",
                                "--disable-blink-features=AutomationControlled",
                                "--disable-infobars"
                        ))
        );

        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setTimeout(transactionTimeout);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.transactionTemplate.setReadOnly(false);
    }

    @PreDestroy
    public void shutdownBrowser() {
        if (browser != null) {
            browser.close();
        }
    }

    @Override
    public List<OrderDTO> mapItems(String link, Long siteSourceJobId, Category category, Subcategory subCategory) {
        if (!active)
            return new ArrayList<>();

        try {
            BrowserContext context = browser.newContext(
                    new Browser.NewContextOptions()
                            .setViewportSize(1920, 1080)
                            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                    "Chrome/120.0.0.0 Safari/537.36")
            );

            context.addInitScript(
                    "Object.defineProperty(navigator, 'webdriver', { get: () => undefined })"
            );

            Page page = context.newPage();
            page.navigate(tasksUrl);

            // Ждём появления списка категорий
            page.waitForSelector("ul.Categories_container__9z_KX");
            // Сброс всех категорий
            page.locator("label.Checkbox_label__uNY3B:has-text(\"Все категории\")").click();
            page.waitForTimeout(30000);

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
                log.debug("YouDo: no order elements found");
                context.close();
                return List.of();
            }

            log.info("YouDo: найдено {} заказов", elementsOrders.size());

            // Парсим все заказы (без сохранения в БД)
            List<Order> parsedOrders = elementsOrders.stream()
                    .map(e -> parseOrder(e, siteSourceJobId, category, subCategory))
                    .filter(Objects::nonNull)
                    .filter(Order::isValidOrder)
                    .collect(Collectors.toList());

            log.info("YouDo: успешно распаршено {} заказов", parsedOrders.size());

            // BATCH обработка
            List<OrderDTO> result = new ArrayList<>();
            int totalBatches = (int) Math.ceil((double) parsedOrders.size() / batchSize);

            for (int batchNum = 0; batchNum < totalBatches; batchNum++) {
                int start = batchNum * batchSize;
                int end = Math.min(start + batchSize, parsedOrders.size());
                List<Order> batch = parsedOrders.subList(start, end);

                log.debug("YouDo: обработка batch {}/{}, заказы {}-{}",
                        batchNum + 1, totalBatches, start, end);

                List<OrderDTO> batchResult = processBatch(batch, category, subCategory);
                result.addAll(batchResult);

                log.debug("YouDo: batch {}/{} обработан, сохранено {} заказов",
                        batchNum + 1, totalBatches, batchResult.size());
            }

            context.close();
            log.info("YouDo: обработка завершена, всего сохранено {} новых заказов", result.size());
            return result;

        } catch (Exception e) {
            log.error("Playwright error: YouDoOrderParser", e);
            return List.of();
        }
    }

    public List<OrderDTO> processBatch(List<Order> batch, Category category, Subcategory subCategory) {
        return transactionTemplate.execute(status -> {
            if (batch.isEmpty()) {
                return List.of();
            }

            // 1. Batch проверка существующих заказов
            List<String> links = batch.stream()
                    .map(Order::getLink)
                    .collect(Collectors.toList());

            Set<String> existingLinks = orderRepository.findExistingLinks(
                    links,
                    category.getId(),
                    subCategory != null ? subCategory.getId() : null
            );

            log.debug("YouDo batch: найдено {} существующих заказов из {}",
                    existingLinks.size(), batch.size());

            // 2. Фильтруем и сохраняем только новые
            return batch.stream()
                    .filter(order -> !existingLinks.contains(order.getLink()))
                    .map(order -> saveOrderInTransaction(order, category, subCategory))
                    .collect(Collectors.toList());
        });
    }

    private OrderDTO saveOrderInTransaction(Order order, Category category, Subcategory subCategory) {
        // Сохранение в рамках транзакции
        service.saveOrderLinks(category, subCategory, order.getLink());

        ParserSource ps = order.getSourceSite();
        ParserSource existing = parserSourceRepository
                .findBySourceAndCategoryAndSubCategory(
                        ps.getSource(),
                        ps.getCategory(),
                        ps.getSubCategory()
                )
                .orElseGet(() -> parserSourceRepository.save(ps));

        order.setSourceSite(existing);
        Order savedOrder = orderRepository.save(order);

        OrderDTO dto = mapper.map(savedOrder, OrderDTO.class);
        SourceSiteDTO source = dto.getSourceSite();
        source.setCategoryName(category.getNativeLocName());
        if (subCategory != null)
            source.setSubCategoryName(subCategory.getNativeLocName());
        dto.setSourceSite(source);

        return dto;
    }

    private void clickCategory(Page page, String categoryName) {
        page.waitForSelector("ul[class*='Categories_container']",
                new Page.WaitForSelectorOptions().setTimeout(10000));

        Locator category = page.locator(
                "//label[contains(@class,'Checkbox_label')][contains(.,'" + categoryName + "')]"
        );
        category.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        category.click();
    }

    private void clickSubCategory(Page page, String categoryName, String subCategoryName) {
        Locator categoryBlock = page.locator(
                "//li[.//label[contains(@class,'Checkbox_label')][contains(.,'" + categoryName + "')]]"
        );

        Locator arrow = categoryBlock.locator("span[class*='Categories_arrow']");
        if (categoryBlock.locator("ul.Categories_subList__iasnn.hidden").count() > 0) {
            arrow.click();
        }

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
        Order order = new Order();

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

        // Создаём ParserSource
        ParserSource parserSource = new ParserSource();
        parserSource.setSource(siteSourceJobId);
        parserSource.setCategory(category.getId());
        parserSource.setSubCategory(subCategory != null ? subCategory.getId() : null);
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