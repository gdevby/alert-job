package by.gdev.alert.job.parser.service.order;

import by.gdev.alert.job.parser.cloudflare.CloudflareDetector;
import by.gdev.alert.job.parser.domain.currency.Currency;
import by.gdev.alert.job.parser.domain.db.Category;
import by.gdev.alert.job.parser.domain.db.CurrencyEntity;
import by.gdev.alert.job.parser.domain.db.Order;
import by.gdev.alert.job.parser.domain.db.ParserSource;
import by.gdev.alert.job.parser.domain.db.Price;
import by.gdev.alert.job.parser.domain.db.Subcategory;
import by.gdev.alert.job.parser.proxy.service.ProxyService;
import by.gdev.alert.job.parser.repository.CurrencyRepository;
import by.gdev.alert.job.parser.repository.OrderRepository;
import by.gdev.alert.job.parser.repository.ParserSourceRepository;
import by.gdev.alert.job.parser.service.ParserService;
import by.gdev.alert.job.parser.util.SiteName;
import by.gdev.alert.job.parser.util.proxy.ProxyCredentials;
import by.gdev.common.model.OrderDTO;
import by.gdev.common.model.SourceSiteDTO;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Proxy;
import com.microsoft.playwright.options.WaitUntilState;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class FreelancehuntOrderParser extends AbsctractSiteParser {

    private final ParserService service;
    private final OrderRepository orderRepository;
    private final ParserSourceRepository parserSourceRepository;
    private final CurrencyRepository currencyRepository;
    private final ProxyService proxyService;
    private final ModelMapper mapper;

    private static final String JOBS_LINK = "https://freelancehunt.com/jobs";

    @Override
    @SneakyThrows
    public List<OrderDTO> mapItems(String link, Long siteSourceJobId, Category category, Subcategory subCategory) {
        boolean isCloudflareProtection = CloudflareDetector.hasCloudflareProtection(JOBS_LINK);
        //если страница не отдается и включена cloudflare на сайте
        if (isCloudflareProtection){
            return mapItemsInternalCloudflare(siteSourceJobId, category, subCategory);
        }

        return new ArrayList<>();
    }

    private List<OrderDTO> mapItemsInternalCloudflare(Long siteSourceJobId, Category category, Subcategory subCategory){
        try (Playwright playwright = Playwright.create()) {
            ProxyCredentials randomProxy = proxyService.getRandomActiveProxy();

            BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                    .setHeadless(true) // в докере всегда headless
                    .setProxy(new Proxy("http://" + randomProxy.getHost() + ":" + randomProxy.getPort())
                            .setUsername(randomProxy.getUsername())
                            .setPassword(randomProxy.getPassword())
                    )
                    .setArgs(Arrays.asList(
                            "--disable-blink-features=AutomationControlled",
                            "--no-sandbox",
                            "--disable-dev-shm-usage",
                            "--window-size=1920,1080"
                    ));

            Browser browser = playwright.chromium().launch(launchOptions);

            // создаём контекст с «человечным» профилем
            Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
                    .setViewportSize(1920, 1080)
                    .setLocale("ru-RU")
                    .setTimezoneId("Europe/Minsk")
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                            + "AppleWebKit/537.36 (KHTML, like Gecko) "
                            + "Chrome/120.0.0.0 Safari/537.36");

            BrowserContext context = browser.newContext(contextOptions);

            // убираем маркеры автоматизации
            context.addInitScript(
                    "Object.defineProperty(navigator, 'webdriver', {get: () => undefined});" +
                            "window.chrome = { runtime: {} };" +
                            "Object.defineProperty(navigator, 'plugins', {get: () => [1,2,3]});" +
                            "Object.defineProperty(navigator, 'languages', {get: () => ['ru-RU','ru','en-US','en']});"
            );

            Page page = context.newPage();
            page.navigate(JOBS_LINK, new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));

            // выбираем все блоки заказов
            Locator elementsOrders = page.locator("div.job-list-item");
            System.out.println("Found Freelancehunt orders: " + elementsOrders.count());
            List<OrderDTO> orders = elementsOrders.all().stream()
                    .map(e -> parseOrder(e, siteSourceJobId, category, subCategory))
                    .filter(Objects::nonNull)
                    .filter(Order::isValidOrder)
                    .filter(order -> !orderRepository.existsByLink(order.getLink()))
                    .map(order -> saveOrder(order, category, subCategory))
                    .toList();

            browser.close();
            return orders;
        }catch (Exception e) {
            log.error("Playwright error: FreelancehuntOrderParser", e);
            return List.of();
        }
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

    private Order parseOrder(Locator item, Long siteSourceJobId, Category category, Subcategory subCategory) {

        // Заголовок и ссылка
        Locator titleEl = item.locator("a.job-name.with-highlights");
        String link = null;
        String title = null;
        if (titleEl.count() > 0) {
            link = titleEl.getAttribute("href");
            title = titleEl.textContent();
        }
        else {
            return null;
        }

        Order order = orderRepository.findByLink(link).orElseGet(Order::new);
        order.setTitle(title);
        order.setLink(link);
        // Описание
        Locator descEl = item.locator("div.job-description.with-highlights");
        if (descEl.count() > 0) {
            order.setMessage(descEl.textContent());
        }

        // Компания
        Locator companyEl = item.locator("div.company a.company-name");
        if (companyEl.count() > 0) {
            // order.setCompany(companyEl.textContent());
        }

        // Дата публикации (относительное время)
        Locator dateEl = item.locator("div.job-publication-time > span");
        if (dateEl.count() > 0) {
            String relative = dateEl.textContent();
            order.setDateTime(parseRelativeDate(relative));
        } else {
            order.setDateTime(new Date());
        }

        // Цена
        Locator priceEl = item.locator("div.job-price");
        if (priceEl.count() > 0) {
            String priceText = priceEl.textContent();
            CurrencyEntity ce = currencyRepository.findByCurrencyCode(
                    priceText.contains("USD") ? Currency.USD.name() : Currency.UAH.name()
            ).orElse(null);

            if (ce != null) {
                String cleaned = priceText.replace("\u202F", "").replace("от ", "");
                String[] parts = cleaned.replaceAll("[^0-9]", " ").trim().split("\\s+");
                if (parts.length > 0) {
                    int nominalAmount = Integer.parseInt(parts[0]);
                    double priceValue = (nominalAmount / ce.getNominal()) * ce.getCurrencyValue();
                    order.setPrice(new Price(priceText, (int) priceValue));
                }
            }
        }

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
            } else if (cleaned.contains("дн")) { // день/дня/дней
                cal.add(Calendar.DAY_OF_MONTH, -n);
            } else if (cleaned.contains("нед")) {
                cal.add(Calendar.WEEK_OF_YEAR, -n);
            } else if (cleaned.contains("месяц")) {
                cal.add(Calendar.MONTH, -n);
            } else {
                return new Date();
            }
            return cal.getTime();
        } catch (Exception ex) {
            return new Date();
        }
    }


    @Override
    public SiteName getSiteName() {
        return SiteName.FREELANCEHUNT;
    }
}