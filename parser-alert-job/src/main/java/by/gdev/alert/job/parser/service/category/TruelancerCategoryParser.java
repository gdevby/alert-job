package by.gdev.alert.job.parser.service.category;

import by.gdev.alert.job.parser.domain.db.SiteSourceJob;
import by.gdev.alert.job.parser.domain.truelancer.TrueLancerCategory;
import by.gdev.alert.job.parser.domain.truelancer.TruelancerCategoriesResponse;
import by.gdev.alert.job.parser.factory.RestTemplateFactory;
import by.gdev.alert.job.parser.util.SiteName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class TruelancerCategoryParser implements CategoryParser{

    @Value("${truelancer.proxy.active}")
    private boolean truelancerProxyActive;
    private final RestTemplateFactory restTemplateFactory;

    @Override
    public Map<ParsedCategory, List<ParsedCategory>> parse(SiteSourceJob siteSourceJob) {
        RestTemplate restTemplate = restTemplateFactory.getRestTemplate(truelancerProxyActive);  
 
        TruelancerCategoriesResponse response = restTemplate.postForObject("https://api.truelancer.com/api/v1/categories", null, TruelancerCategoriesResponse.class);

        Map<String, TrueLancerCategory> categories = response.getCategories();
        Map<ParsedCategory, List<ParsedCategory>> result = new LinkedHashMap<>();
        for (Map.Entry<String, TrueLancerCategory> entry : categories.entrySet()) {
            String categoryName = entry.getValue().getCategory();
            log.debug("found category {}", categoryName);
            ParsedCategory parsedCategory = new ParsedCategory(null, categoryName, null, entry.getKey());
            List<ParsedCategory> subCategories = entry.getValue().getSubCategories().entrySet().stream()
                    .map(subCategory -> {
                        String subCategoryName = subCategory.getValue();
                        log.debug("found subcategory {}", subCategoryName);
                        return new ParsedCategory(null, subCategoryName, null, subCategory.getKey());
                    })
                    .toList();
            result.put(parsedCategory, subCategories);
        }
        return result;
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.TRUELANCER;
    }
}
