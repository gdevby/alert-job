package by.gdev.alert.job.parser.service.category;

import by.gdev.alert.job.parser.domain.db.SiteSourceJob;
import by.gdev.alert.job.parser.util.SiteName;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@Slf4j
public class WorkspaceCategoryParser implements CategoryParser {


    private final String baseURL = "https://workspace.ru";

    @Override
    public Map<ParsedCategory, List<ParsedCategory>> parse(SiteSourceJob siteSourceJob) {
        try {
            Document document = Jsoup.connect(siteSourceJob.getParsedURI()).get();

            Elements categoryBar = document.getElementsByClass("filters__group _need vacancies__filters");
            Elements categoryElements = categoryBar.get(0).getElementsByClass("vacancies__filters-check-item");
            Map<ParsedCategory, List<ParsedCategory>> collect = categoryElements.stream()
                    .map(categoryElement -> {
                        String relativePath = categoryElement.getElementsByTag("a").get(0).attr("href");
                        String link = baseURL + relativePath;
                        String categoryName = categoryElement.text();
                        log.debug("found category {}", categoryName);
                        return new ParsedCategory(null, categoryName, null, link);
                    })
                    .collect(Collectors.toMap(
                            parsedCategory -> parsedCategory,
                            parsedCategory -> Collections.emptyList()
                    ));
            return collect;
        } catch (IOException e) {
            log.error("Cannot parse categories by link {}", siteSourceJob.getParsedURI());
            return Map.of();
        }
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.WORKSPACE;
    }
}
