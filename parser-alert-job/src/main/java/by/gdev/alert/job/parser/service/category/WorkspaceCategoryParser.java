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

    @Override
    public Map<ParsedCategory, List<ParsedCategory>> parse(SiteSourceJob siteSourceJob) {
        try {
            Document document = Jsoup.connect(siteSourceJob.getParsedURI()).get();

            Elements elements = document.getElementsByClass("main-new-list__item _small _arrow-right");

            Map<ParsedCategory, List<ParsedCategory>> collect = elements.stream()
                    .map(element -> {
                        String link = element.attr("href");
                        String categoryName = element.child(1).child(0).text();
                        log.debug("found category {}", categoryName);
                        return new ParsedCategory(null, categoryName, null, link);
                    })
                    .collect(Collectors.toMap(
                            parsedCategory -> parsedCategory,
                            parsedCategory -> Collections.emptyList()
                    ));
            return collect;
        } catch (IOException e) {
            log.error("Cannot parse {} site", siteSourceJob.getParsedURI());
            return Map.of();
        }
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.WORKSPACE;
    }
}
