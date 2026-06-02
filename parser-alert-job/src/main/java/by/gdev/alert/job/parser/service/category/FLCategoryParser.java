package by.gdev.alert.job.parser.service.category;

import by.gdev.alert.job.parser.domain.db.SiteSourceJob;
import by.gdev.alert.job.parser.domain.parsing.FlCategories;
import by.gdev.alert.job.parser.domain.parsing.FlCategoryItem;
import by.gdev.alert.job.parser.factory.RestTemplateFactory;
import by.gdev.alert.job.parser.util.SiteName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;


@Service
@Slf4j
@RequiredArgsConstructor
public class FLCategoryParser implements CategoryParser{

    private String categoriesLinkFl = "https://www.fl.ru/prof_groups/";
    private String subcategoriesLink = "https://www.fl.ru/prof_groups/professions/?prof_group_id=%s";
    private String flRss = "https://www.fl.ru/rss/all.xml?category=%s";
    private String flRssWithSubcategory = "https://www.fl.ru/rss/all.xml?subcategory=%s&category=%s";

    private final RestTemplateFactory restTemplateFactory;

    @Override
    public Map<ParsedCategory, List<ParsedCategory>> parse(SiteSourceJob siteSourceJob) {
        Map<ParsedCategory, List<ParsedCategory>> map = new LinkedHashMap<>();
        RestTemplate restTemplate = restTemplateFactory.getRestTemplate(false);
        FlCategories flCategories = restTemplate.getForObject(categoriesLinkFl, FlCategories.class);
        flCategories.items().stream()
                .sorted(Comparator.comparing(FlCategoryItem::rank))
                .forEach(item -> {
                    ParsedCategory c = new ParsedCategory(
                            item.name_en(),
                            item.name(),
                            (long) item.id(),
                            String.format(flRss, item.id())
                    );
                    log.debug("found category {} {} {} for site {}", c.id(), c.translatedName(), c.rss(), getSiteName());
                    List<ParsedCategory> listSubcat = new ArrayList<>();
                    map.put(c, listSubcat);

                    FlCategories flCategories1 = restTemplate.getForObject(
                            String.format(subcategoriesLink, item.id()),
                            FlCategories.class
                    );

                    flCategories1.items().stream()
                            .sorted(Comparator.comparing(FlCategoryItem::rank))
                            .forEach(ee -> {
                                ParsedCategory pc = new ParsedCategory(
                                        ee.name_en(),
                                        ee.name(),
                                        (long) ee.id(),
                                        String.format(flRssWithSubcategory, ee.id(), c.id())
                                );
                                listSubcat.add(pc);
                                log.debug("found subcategory {} {} {} for site {}", pc.id(), pc.translatedName(), pc.rss(), getSiteName());
                            });
                    log.debug("subcategory size {}", listSubcat.size());
                });
        return map;
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.FLRU;
    }

}
