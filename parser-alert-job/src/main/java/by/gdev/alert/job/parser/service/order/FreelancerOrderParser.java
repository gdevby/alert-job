package by.gdev.alert.job.parser.service.order;


import by.gdev.alert.job.parser.domain.currency.Currency;
import by.gdev.alert.job.parser.domain.db.*;
import by.gdev.alert.job.parser.service.playwright.PlaywrightSiteParser;
import by.gdev.alert.job.parser.util.Pair;
import by.gdev.alert.job.parser.util.SiteName;

import by.gdev.alert.job.parser.util.proxy.ProxyCredentials;
import by.gdev.common.model.OrderDTO;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;


@Service
@RequiredArgsConstructor
@Slf4j
public class FreelancerOrderParser extends PlaywrightSiteParser {

    @Value("${freelancer.proxy.active}")
    private boolean freelancerProxyActive;

    private static boolean HEADLESS = false;

    @Value("${parser.work.freelancer.com}")
    private void setActive(boolean active) {
        this.active = active;
    }

    @Override
    protected List<OrderDTO> mapItems(String link, Long siteSourceJobId, Category category, Subcategory subCategory) {
        if (!active)
            return new ArrayList<>();
        return mapItemsWithRetry(link, freelancerProxyActive, siteSourceJobId , category, subCategory);
    }

    @Override
    protected List<OrderDTO> mapItems(String link, Long siteSourceJobId, List<Pair<Category, Subcategory>> categoriesPairList) {
        List<OrderDTO> orders = new ArrayList<>();
        if (!active)
            return orders;

        PlaywrightSession session = null;
        try {
            session = createSession(HEADLESS, freelancerProxyActive);
            Page page = session.getPage();
            for(Pair<Category, Subcategory> pair: categoriesPairList){
                List<OrderDTO> categoryOrders = mapItemsWithRetry(link, freelancerProxyActive, siteSourceJobId , pair, page);
                orders.addAll(categoryOrders);
            }
        }
        finally {
            closeResources(session.getPage(), session.getContext(), session.getBrowser(), session.getPlaywright());
        }
        return orders;
    }

    private List<OrderDTO> tasksParsing(Page page,
                                        Long siteSourceJobId,
                                        Category category,
                                        Subcategory subCategory) {

        page.waitForSelector("div.JobSearchCard-item",
                new Page.WaitForSelectorOptions().setTimeout(30000));

        Locator elementsOrders = page.locator("div.JobSearchCard-item");

        List<Order> parsedOrders = elementsOrders.all()
                .stream()
                .map(e -> parseOrder(e, siteSourceJobId, category, subCategory))
                .filter(Objects::nonNull)
                .toList();

        List<OrderDTO> orders = getOrdersData(parsedOrders, category, subCategory);

        orders.forEach(order ->
                log.info("*** order: {} , result {}",
                        order.getTitle(),
                        getParserService().isExistsOrder(category, subCategory, order.getLink()))
        );

        return orders;
    }


    public void clickCategory(Page page, Category category, Subcategory subCategory) {
        String url;
        if (subCategory != null){
            url = subCategory.getLink();
        }
        else {
            url = category.getLink();
        }

        page.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));
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
            browser = createBrowser(playwright, proxy, true, freelancerProxyActive);
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

            List<OrderDTO> orders = getOrdersData(parsedOrders, category, subCategory);
            return orders;
        }
        finally {
            closeResources(page, context, browser, playwright);
        }
    }

    private Order parseOrder(Locator item,
                             Long siteSourceJobId,
                             Category category,
                             Subcategory subCategory) {

        // --- Заголовок и ссылка ---
        Locator titleEl = item.locator("a.JobSearchCard-primary-heading-link");
        if (titleEl.count() == 0) return null;

        String link = titleEl.getAttribute("href");
        if (link != null && !link.startsWith("http")) {
            link = "https://www.freelancer.com" + link;
        }

        String title = titleEl.innerText().trim();

        // Ищем существующий заказ или создаём новый
        Order order = getOrderRepository().findByLink(link).orElseGet(Order::new);
        order.setTitle(title);
        order.setLink(link);

        // --- Описание ---
        Locator descEl = item.locator("p.JobSearchCard-primary-description");
        String description = descEl.count() > 0 ? descEl.innerText().trim() : null;
        order.setMessage(description);

        // --- Дата ---
        order.setDateTime(new Date());

        // --- Цена ---
        parsePrice(order, item); // твой стандартный метод

        // --- Источник ---
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

        // --- Статусы ---
        order.setValidOrder(true);
        order.setOpenForAll(true);

        return order;
    }

    private void parsePrice(Order order, Locator item) {
        try {
            Locator priceEl = item.locator("div.JobSearchCard-secondary-price");
            String priceText = null;

            if (priceEl.count() > 0) {
                priceText = priceEl.innerText().trim();
            }

            if ((priceText == null || priceText.isEmpty())) {
                Locator hiddenPrice = item.locator("div.JobSearchCard-primary-price");
                if (hiddenPrice.count() > 0) {
                    priceText = hiddenPrice.innerText().trim();
                }
            }

            if (priceText == null || priceText.isEmpty()) {
                return; // цены нет
            }

            // Извлекаем число
            String nominalStr = priceText.replaceAll("[^0-9]", "");
            if (nominalStr.isEmpty()) return;

            int nominal = Integer.parseInt(nominalStr);

            // Конвертация валюты (USD → твоя валюта)
            CurrencyEntity ce = getCurrencyRepository()
                    .findByCurrencyCode(Currency.USD.name())
                    .orElse(null);

            if (ce != null) {
                double priceValue = (nominal / ce.getNominal()) * ce.getCurrencyValue();
                order.setPrice(new Price("$" + nominal, (int) priceValue));
            }

        } catch (Exception ignored) {}
    }



    @Override
    public SiteName getSiteName() {
        return SiteName.FREELANCER;
    }
}