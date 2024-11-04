package by.gdev.alert.job.parser.service.category;

import by.gdev.alert.job.parser.domain.db.SiteSourceJob;
import by.gdev.alert.job.parser.util.SiteName;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FreelanceRuCategoryParser implements CategoryParser {

    private String freelanceRuRss = "https://freelance.ru/rss/index";
    private String freelanceRuRssFeed = "https://freelance.ru/rss/feed/list/s.%s";
    private String freelanceRuRssFeedSubcategories = "https://freelance.ru/rss/feed/list/s.%s.f.%s";
    private static final String VALUE = "value";

    @Override
    public Map<ParsedCategory, List<ParsedCategory>> parse(SiteSourceJob siteSourceJob) {
        try {
            Document doc = Jsoup.connect(freelanceRuRss).get();

            Elements res = doc.getElementById("spec-selector-id").children();
            Map<ParsedCategory, List<ParsedCategory>> result = new LinkedHashMap<>();
            res.stream()
                    .map(element -> {
                        ParsedCategory parsedCategory = new ParsedCategory(
                                null,
                                element.text(),
                                Long.valueOf(element.attr(VALUE)),
                                String.format(freelanceRuRssFeed, element.attr(VALUE))
                        );
                        log.debug("found category {} {} {}", parsedCategory.id(), parsedCategory.translatedName(), parsedCategory.rss());
                        return parsedCategory;
                    })
                    .filter(parsedCategory -> !parsedCategory.id().equals(0L))
                    .peek(parsedCategory ->
                            result.put(parsedCategory,
                                    doc.getElementById("spec-" + parsedCategory.id()).select("label").stream()
                                    .map(element -> {
                                        Long id = Long.valueOf(element.child(0).attr("value"));
                                        return new ParsedCategory(
                                                null,
                                                element.text(),
                                                id,
                                                String.format(freelanceRuRssFeedSubcategories, parsedCategory.id(), id));
                                    })
                                    .filter(subCategory -> !subCategory.id().equals(parsedCategory.id()))
                                    .peek(subCategory -> log.debug("found subcategory {} {} {} ", subCategory.id(), subCategory.translatedName(), subCategory.rss()))
                                    .collect(Collectors.toList())
                            )
                    ).findAny();
            return result;
        } catch (IOException e) {
            log.error("cannot parse {} site", siteSourceJob.getParsedURI());
            throw new RuntimeException(e);
        }
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.FREELANCERU;
    }
}
