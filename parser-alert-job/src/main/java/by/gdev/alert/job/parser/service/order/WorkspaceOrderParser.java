package by.gdev.alert.job.parser.service.order;

import by.gdev.alert.job.parser.domain.db.*;
import by.gdev.alert.job.parser.service.playwright.PlaywrightSiteParser;
import by.gdev.alert.job.parser.service.playwright.CaptchaService;
import by.gdev.common.model.OrderDTO;
import by.gdev.common.model.SiteName;
import by.gdev.common.util.Pair;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitUntilState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class WorkspaceOrderParser extends PlaywrightSiteParser {

    private final String baseUrl = "https://workspace.ru";
    private final String statusParam = "?STATUS=published";

    @Value("${parser.work.workspace.ru}")
    private void setActive(boolean active) {
        this.active = active;
    }

    @Value("${workspace.proxy.active:false}")
    private boolean workspaceProxyActive;

    @Value("${parser.headless.workspace.ru:true}")
    private void setHeadless(boolean headless) {
        this.headless = headless;
    }

    @Value("${workspace.debug:false}")
    private void setDebug(boolean debug) {
        this.debug = debug;
    }

    private final CaptchaService captchaService;

    @Override
    protected List<OrderDTO> mapItems(String link, Long siteSourceJobId, List<Pair<Category, Subcategory>> categoriesPairList) {
        if (!active)
            return List.of();
        PlaywrightSession session = null;
        try {
            session = createSession(headless, workspaceProxyActive);
            Page page = session.getPage();
            safeNavigate(page, link + statusParam);
            boolean solveCaptcha;
            //Проверяем вообще какая сейчас страница - и включена ли капча
            if (captchaService.isCaptchaPage(page)){
                log.debug("Обнаружена SmartCaptcha на {} — пытаемся пройти...", getSiteName());
                //Проверяем можем ли мы пройти капчу
                solveCaptcha = captchaService.solveSmartCaptcha(page);
            }
            else {
                solveCaptcha = true;
            }
            //если капчу прошли значит загрузилась страница заказов и можно парсить заказы
            if (!solveCaptcha){
                log.warn("SmartCaptcha для сайта {} НЕ пройдена — парсинг прекращён", getSiteName());
                return List.of();
            }
            else {
                log.debug("SmartCaptcha для {} успешно пройдена", getSiteName());
            }
            List<OrderDTO> orders = new ArrayList<>();
            for (Pair<Category, Subcategory> pair : categoriesPairList) {
                orders.addAll(
                        mapItemsWithRetry(link, workspaceProxyActive, siteSourceJobId, pair, page)
                );
            }
            return orders;
        } finally {
            if (session != null) {
                closeResources(session.getPage(), session.getContext(), session.getBrowser(), session.getPlaywright());
            }
        }
    }

    @Override
    protected List<OrderDTO> mapPlaywrightItems(String link, Long siteSourceJobId, Pair<Category, Subcategory> pair, Page page) {
        clickWorkspaceCategory(page, pair.getLeft());
        List<OrderDTO> orders = parseWorkspace(page, siteSourceJobId, pair.getLeft(), pair.getRight());
        safeNavigate(page, link + statusParam);
        return orders;
    }

    private void safeNavigate(Page page, String url) {
        for (int i = 1; i <= 5; i++) {
            try {
                page.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
                return;
            } catch (PlaywrightException e) {
                log.warn("Навигация не удалась (попытка {}): {}", i, e.getMessage());
                page.waitForTimeout(1500);
            }
        }
        throw new RuntimeException("Не удалось открыть страницу после 5 попыток: " + url);
    }

    private void clickWorkspaceCategory(Page page, Category category) {
        String name = category.getNativeLocName().trim();
        // Ждём появления блока фильтров
        page.waitForSelector("div.vacancies__filters");
        // Ищем ссылку по тексту
        Locator link = page.locator("a.vacancies__filters-check-item-line:has-text('" + name + "')");
        // Ждём появления ссылки
        link.waitFor(new Locator.WaitForOptions().setTimeout(10000));
        // Кликаем
        link.click();
        // Ждём загрузку страницы
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        page.waitForTimeout(300);
        log.debug("Клик по категории '{}' выполнен", name);
    }

    private List<OrderDTO> parseWorkspace(Page page, Long siteSourceJobId, Category category, Subcategory subCategory) {
        page.waitForTimeout(1500);
        Locator cards = page.locator("div.vacancies__card._tender");
        int count = cards.count();
        if (count == 0)
            return List.of();
        List<Order> rawOrders = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Order order = parseOrder(cards.nth(i), siteSourceJobId, category, subCategory);
            if (order != null)
                rawOrders.add(order);
        }
        return getOrdersData(rawOrders, category, subCategory);
    }

    private Order parseOrder(Locator card, Long siteSourceJobId, Category category, Subcategory subCategory) {
        //Заголовок
        Locator titleEl = card.locator(".b-tender__title a");
        if (titleEl.count() == 0)
            return null;

        String title = titleEl.innerText().trim();
        String postfixLink = titleEl.getAttribute("href");
        //Ccылка на заказ
        String fullLink = baseUrl + postfixLink;
        // Проверка уникальности
        if (!getParserService().isExistsOrder(category, subCategory, fullLink))
            return null;
        Order order = getOrderRepository().findOrdersByLink(fullLink)
                .stream().findFirst().orElseGet(Order::new);
        order.setTitle(title);
        order.setLink(fullLink);

        //Цена
        String priceText = null;
        Locator infoItems = card.locator(".b-tender__info-item");
        for (int i = 0; i < infoItems.count(); i++) {
            Locator titleBlock = infoItems.nth(i).locator(".b-tender__info-item-title");
            Locator textBlock = infoItems.nth(i).locator(".b-tender__info-item-text");
            if (titleBlock.count() == 0 && textBlock.count() > 0) {
                String txt = textBlock.innerText().trim();
                if (txt.matches(".*\\d+.*")) {
                    priceText = txt;
                    break;
                }
            }
        }
        if (priceText != null) {
            order.setPrice(parsePrice(priceText));
        }

        //Дата заказа
        Date date = new Date();
        for (int i = 0; i < infoItems.count(); i++) {
            Locator titleBlock = infoItems.nth(i).locator(".b-tender__info-item-title");
            Locator textBlock = infoItems.nth(i).locator(".b-tender__info-item-text");

            if (titleBlock.count() == 0 || textBlock.count() == 0)
                continue;

            String label = titleBlock.innerText().trim();
            String value = textBlock.innerText().trim();
            if (label.equalsIgnoreCase("Опубликован")) {
                try {
                    SimpleDateFormat formatter =
                            new SimpleDateFormat("dd MMMM yyyy", Locale.forLanguageTag("ru"));
                    date = formatter.parse(value);
                } catch (Exception ignored) {}
            }
        }

        order.setDateTime(date);
        //Источник
        ParserSource ps = new ParserSource();
        ps.setSource(siteSourceJobId);
        ps.setCategory(category.getId());
        ps.setSubCategory(subCategory != null ? subCategory.getId() : null);
        order.setSourceSite(ps);
        //Заказ открыт для всех
        order.setOpenForAll(true);
        return order;
    }

    private Price parsePrice(String priceText) {
        if (priceText == null || priceText.isBlank())
            return new Price(priceText, 0);
        // Убираем неразрывные пробелы
        String cleaned = priceText.replace("\u00A0", " ").trim();
        int value = 0;
        // --- Формат: "до 100 000" ---
        if (cleaned.startsWith("до")) {
            String digits = cleaned.replaceAll("[^0-9]", "");
            value = digits.isEmpty() ? 0 : Integer.parseInt(digits);
            return new Price(priceText, value);
        }
        // --- Формат: "200 000 - 800 000" ---
        if (cleaned.contains("-")) {
            String[] parts = cleaned.split("-");
            if (parts.length == 2) {
                String right = parts[1].replaceAll("[^0-9]", "");
                value = right.isEmpty() ? 0 : Integer.parseInt(right);
                return new Price(priceText, value);
            }
        }
        // --- Формат: "100 000" ---
        String digits = cleaned.replaceAll("[^0-9]", "");
        value = digits.isEmpty() ? 0 : Integer.parseInt(digits);
        return new Price(priceText, value);
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.WORKSPACE;
    }


    //Метод не используется
    @Override
    protected List<OrderDTO> mapPlaywrightItems(String link, Long siteSourceJobId, Category category, Subcategory subCategory) {
        return List.of();
    }

    //Метод не используется
    @Override
    protected List<OrderDTO> mapItems(String link, Long siteSourceJobId, Category category, Subcategory subCategory) {
        return List.of();
    }
}