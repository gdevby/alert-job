package by.gdev.alert.job.parser.service.category;

import by.gdev.alert.job.parser.domain.db.SiteSourceJob;
import by.gdev.alert.job.parser.domain.parsing.FlCategories;
import by.gdev.alert.job.parser.domain.parsing.FlCategoryItem;
import by.gdev.alert.job.parser.factory.RestTemplateFactory;
import by.gdev.common.model.SiteName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class FLCategoryParser implements CategoryParser {

    private static final String CATEGORIES_URL = "https://www.fl.ru/prof_groups/";
    private static final String SUBCATEGORIES_URL = "https://www.fl.ru/prof_groups/professions/?prof_group_id=%s";
    private static final String RSS_CATEGORY = "https://www.fl.ru/rss/all.xml?category=%s";
    private static final String RSS_SUBCATEGORY = "https://www.fl.ru/rss/all.xml?subcategory=%s&category=%s";

    @Value("${flru.proxy.active:false}")
    private boolean proxyActive;

    private final RestTemplateFactory restTemplateFactory;

    @Override
    public Map<ParsedCategory, List<ParsedCategory>> parse(SiteSourceJob siteSourceJob) {
        Map<ParsedCategory, List<ParsedCategory>> result = new LinkedHashMap<>();
        RestTemplate restTemplate = restTemplateFactory.getRestTemplate(proxyActive);

        FlCategories flCategories = restTemplate.getForObject(CATEGORIES_URL, FlCategories.class);
        if (flCategories == null || flCategories.items() == null) {
            log.warn("fl.ru вернул пустой список категорий");
            return result;
        }

        for (FlCategoryItem categoryItem : flCategories.items()) {
            ParsedCategory category = new ParsedCategory(
                    categoryItem.name_en(),
                    categoryItem.name(),
                    (long) categoryItem.id(),
                    String.format(RSS_CATEGORY, categoryItem.id())
            );
            log.debug("found category {} {} {}", category.id(), category.translatedName(), category.rss());

            List<ParsedCategory> subcategories = new ArrayList<>();
            FlCategories subResponse = restTemplate.getForObject(
                    String.format(SUBCATEGORIES_URL, categoryItem.id()),
                    FlCategories.class
            );
            if (subResponse != null && subResponse.items() != null) {
                for (FlCategoryItem subItem : subResponse.items()) {
                    ParsedCategory sub = new ParsedCategory(
                            subItem.name_en(),
                            subItem.name(),
                            (long) subItem.id(),
                            String.format(RSS_SUBCATEGORY, subItem.id(), categoryItem.id())
                    );
                    subcategories.add(sub);
                    log.debug("found subcategory {} {} {}", sub.id(), sub.translatedName(), sub.rss());
                }
            }

            result.put(category, subcategories);
            log.debug("subcategory size {}", subcategories.size());
        }

        return result;
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.FLRU;
    }
}
