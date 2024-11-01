package by.gdev.alert.job.parser.service.category;

import by.gdev.alert.job.parser.domain.db.SiteSourceJob;
import by.gdev.alert.job.parser.util.ParserStringUtils;
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

    @Override
    public Map<ParsedCategory, List<ParsedCategory>> parse(SiteSourceJob siteSourceJob) {
        try {
            Document doc = Jsoup.connect(siteSourceJob.getParsedURI()).get();

            Elements res = doc.getElementById("spec-selector-id").children();
            Map<ParsedCategory, List<ParsedCategory>> result = new LinkedHashMap<>();
            res.stream()
                    .map(fs -> {
                        ParsedCategory c = new ParsedCategory(null, fs.text(), Long.valueOf(fs.attr(ParserStringUtils.VALUE)),
                                String.format(freelanceRuRssFeed, fs.attr(ParserStringUtils.VALUE)));
                        log.debug("found category {} {} {}", c.id(), c.translatedName(), c.rss());
                        return c;
                    })
                    .filter(f -> !f.id().equals(0L))
                    .peek(f -> result.put(f, doc.getElementById("spec-" + f.id()).select("label")
                            .stream()
                            .map(sc -> {
                                Long id = Long.valueOf(sc.child(0).attr("value"));
                                return new ParsedCategory(null, sc.text(), id,
                                        String.format(freelanceRuRssFeedSubcategories, f.id(), id));
                            })
                            .filter(fc -> !fc.id().equals(f.id()))
                            .peek(fc -> log.debug("		found subcategory {} {} {} ", fc.id(), fc.translatedName(), fc.rss()))
                            .collect(Collectors.toList())));
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
