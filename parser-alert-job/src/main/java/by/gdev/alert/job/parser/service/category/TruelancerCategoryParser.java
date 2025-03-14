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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        Map<ParsedCategory, List<ParsedCategory>> result = categories.entrySet().stream()
                .collect(Collectors.toMap(
                        keyMapper -> {
                            String categoryName = keyMapper.getValue().getCategory();
                            log.debug("found category {}", categoryName);
                            return new ParsedCategory(null, categoryName, null, keyMapper.getKey());
                        },
                        valueMapper -> valueMapper.getValue().getSubCategories().entrySet().stream()
                                .map(subCategory -> {
                                    String subCategoryName = subCategory.getValue();
                                    log.debug("found subcategory {}", subCategoryName);
                                    return new ParsedCategory(null, subCategoryName, null, subCategory.getKey());
                                })
                                .collect(Collectors.toList())

                ));
        return result;
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.TRUELANCER;
    }
}
