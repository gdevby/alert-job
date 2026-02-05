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
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitUntilState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import java.util.*;

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

    @Override
    public List<OrderDTO> mapItems(String link, Long siteSourceJobId, Category category, Subcategory subCategory) {
        if (!active)
            return new ArrayList<>();
        return mapItemsWithRetry(link, freelancehuntProxyActive, siteSourceJobId , category, subCategory);
    }

    public void clickCategory(Page page, Category category) {
        String categoryName = category.getNativeLocName();

        // ищем span с нужным текстом
        Locator categorySpan = page.locator("ul.tree li.tree-item span.tree-item-title")
                .filter(new Locator.FilterOptions().setHasText(categoryName));

        if (categorySpan.count() == 0) {
            log.warn("Категория '{}' не найдена на странице FreelancehuntOrderParser", categoryName);
            return;
        }

        // поднимаемся к родительскому <a> и кликаем
        Locator categoryLink = categorySpan.first().locator("xpath=..");
        categoryLink.click(new Locator.ClickOptions().setTimeout(30000));

        // ждём загрузки страницы после клика
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

	private Order parseOrder(Locator item, Long siteSourceJobId, Category category, Subcategory subCategory) {

		// Заголовок и ссылка
		Locator titleEl = item.locator("a.job-name.with-highlights");
		String link = null;
		String title = null;
		if (titleEl.count() > 0) {
			link = titleEl.getAttribute("href");
			title = titleEl.textContent();
		} else {
			return null;
		}

		Order order = getOrderRepository().findByLink(link).orElseGet(Order::new);
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
			CurrencyEntity ce = getCurrencyRepository()
					.findByCurrencyCode(priceText.contains("USD") ? Currency.USD.name() : Currency.UAH.name())
					.orElse(null);

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
		ParserSource parserSource = getParserSourceRepository().findBySourceAndCategoryAndSubCategory(siteSourceJobId,
				category.getId(), subCategory != null ? subCategory.getId() : null).orElseGet(() -> {
					ParserSource ps = new ParserSource();
					ps.setSource(siteSourceJobId);
					ps.setCategory(category.getId());
					ps.setSubCategory(subCategory != null ? subCategory.getId() : null);
					return getParserSourceRepository().save(ps);
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

    @Override
    protected List<OrderDTO> mapPlaywrightItems(String link, Long siteSourceJobId, Category category, Subcategory subCategory) {
        Playwright playwright = null;
        Browser browser = null;
        BrowserContext context = null;
        Page page = null;

        try {
            playwright = createPlaywright();
            ProxyCredentials proxy = getProxyWithRetry(5, 2000);
            browser = createBrowser(playwright, proxy, true, freelancehuntProxyActive);
            context = createBrowserContext(browser, null, false);
            page = context.newPage();
            page.navigate(JOBS_LINK, new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));

            clickCategory(page, category);

            page.waitForSelector("div.job-list-item", new Page.WaitForSelectorOptions().setTimeout(30000));

            Locator elementsOrders = page.locator("div.job-list-item");

            //System.out.println("Found Freelancehunt orders: " + elementsOrders.count());
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
}