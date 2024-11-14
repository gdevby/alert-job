package by.gdev.alert.job.parser.service.category;

import by.gdev.alert.job.parser.domain.db.SiteSourceJob;
import by.gdev.alert.job.parser.util.SiteName;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class WorkanaCategoryParser implements CategoryParser {


    private String baseURL;

    @Override
    public Map<ParsedCategory, List<ParsedCategory>> parse(SiteSourceJob siteSourceJob) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setExtraHTTPHeaders(Map.of("Accept-Language", "en"))
            );
            Page page = context.newPage();


            baseURL = siteSourceJob.getParsedURI();
            page.navigate(baseURL);
            page.waitForLoadState();
            handleCookiePopup(page);


            Map<ParsedCategory, List<ParsedCategory>> categories = extractCategories(page);
            browser.close();
            return categories;
        }
    }

    private void handleCookiePopup(Page page) {
        try {
            page.locator("button#onetrust-pc-btn-handler").click();

            page.waitForSelector("button.ot-pc-refuse-all-handler", new Page.WaitForSelectorOptions().setTimeout(5000));

            page.locator("button.ot-pc-refuse-all-handler").click();
        } catch (Exception e) {
            System.out.println("Popup handling failed or no popup detected: " + e.getMessage());
        }
    }

    public Map<ParsedCategory, List<ParsedCategory>> extractCategories(Page page) {

        List<Locator> categories = page.locator("ul.search-category > li").all();

        return categories.stream()
                .map(categoryLocator -> {

                    String categoryName = categoryLocator.textContent();

                    Locator checkbox = page.locator("label").filter(new Locator.FilterOptions().setHasText(categoryName));
                    Locator input = categoryLocator.locator("input[type='checkbox']");

                    String categoryValue = input.getAttribute("value");
                    String categoryURL = getURL(categoryValue);
                    checkbox.click();

                    ParsedCategory parsedCategory = new ParsedCategory(null, categoryName, null, categoryURL);
                    log.debug("found category {}, {}", categoryName, categoryURL);


                    List<Locator> subcategoryLocators = categoryLocator.locator("ul > li").all();
                    List<ParsedCategory> subCategories = subcategoryLocators.stream()
                            .map(subcategoryLocator -> {
                                String subCategoryName = subcategoryLocator.innerText();
                                Locator input1 = subcategoryLocator.locator("input[type='checkbox']");
                                String subCategoryValue = input1.getAttribute("value");
                                String subCategoryURL = getURL(categoryValue, subCategoryValue);

                                log.debug("found category {}, {}", subCategoryName, subCategoryURL);

                                return new ParsedCategory(null, subCategoryName, null, subCategoryURL);
                            })
                            .toList();

                    checkbox.click();
                    return new AbstractMap.SimpleEntry<>(parsedCategory, subCategories);
                })
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
    }

    private String getURL(String categoryValue) {
        if (!categoryValue.isEmpty()) {
            return baseURL + "?category=" + categoryValue;
        }
        return baseURL;
    }

    private String getURL(String categoryValue, String subCategoryValue) {
        return baseURL + "?category=" + categoryValue + "&subcategory=" + subCategoryValue;
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.WORKANA;
    }
}
