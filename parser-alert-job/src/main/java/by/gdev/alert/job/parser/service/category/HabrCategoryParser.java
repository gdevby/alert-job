package by.gdev.alert.job.parser.service.category;

import by.gdev.alert.job.parser.domain.db.SiteSourceJob;
import by.gdev.alert.job.parser.util.ParserStringUtils;
import by.gdev.alert.job.parser.util.SiteName;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class HabrCategoryParser implements CategoryParser{
    @Override
    public Map<ParsedCategory, List<ParsedCategory>> parse(SiteSourceJob siteSourceJob) {
        Document doc = null;
        try {
            doc = Jsoup.connect(siteSourceJob.getParsedURI()).get();
        } catch (IOException e) {
            log.error("cannot parse {} site", siteSourceJob.getParsedURI());
            throw new RuntimeException(e);
        }

        Elements res = doc.getElementsByClass("category-group__folder");
        return res.stream().map(ee -> {
            String categoryString = ee.getElementsByClass("link_dotted js-toggle").text();
            String engNameCategory = ee.getElementsByClass("checkbox_flat").attr("for");
            ParsedCategory catNew = new ParsedCategory(engNameCategory, categoryString, null, null);
            log.debug("found category {},{}, {}", siteSourceJob.getName(), categoryString, engNameCategory);
            List<ParsedCategory> subList = ee.getElementsByClass("sub-categories__item").stream().map(sub -> {
                Element el = sub.select("input[value]").first();
                ParsedCategory subCategory = new ParsedCategory(el.attr("value"), sub.text(), null, null);
                log.debug("		found subcategory {}, {},{}, {}", siteSourceJob.getName(), subCategory.name(),
                        subCategory.translatedName(), subCategory.name());
                return subCategory;
            }).collect(Collectors.toList());
            log.debug("			subcategory size {}", subList.size());
            return new AbstractMap.SimpleEntry<>(catNew, subList);
        }).collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue, (u, v) -> {
            throw new IllegalStateException(String.format(ParserStringUtils.DUPBLICATE_KEY, u));
        }, LinkedHashMap::new));    }

    @Override
    public SiteName getSiteName() {
        return SiteName.HABR;
    }

}
