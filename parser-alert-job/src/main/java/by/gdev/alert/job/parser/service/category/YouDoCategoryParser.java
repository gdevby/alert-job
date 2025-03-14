package by.gdev.alert.job.parser.service.category;

import by.gdev.alert.job.parser.domain.db.SiteSourceJob;
import by.gdev.alert.job.parser.util.SiteName;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
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
public class YouDoCategoryParser implements CategoryParser{
    @Override
    public Map<ParsedCategory, List<ParsedCategory>> parse(SiteSourceJob siteSourceJob) {
        Document doc = null;
        try {
            doc = Jsoup.connect(siteSourceJob.getParsedURI()).get();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }        
        Element page = doc.getElementsByClass("TasksRedesignPage_categories__xYkgi").get(0);
        Elements el = page.getElementsByClass("Categories_item__nyDMJ");
        
        if (el.size() == 0) {
        	throw new RuntimeException("Parent element not found");
        }
        return el.stream().map(e -> {
            Element elemCategory = e.selectFirst("label.Checkbox_label__uNY3B");
            Elements subEl = e.getElementsByClass("Categories_subList__iasnn");
            List<ParsedCategory> subCategory = subEl.stream()
                    .flatMap(sub -> sub.select("li.Categories_subItem__kkuRq").stream()).map(e1 -> {
                        Element input = e1.selectFirst("input.Checkbox_checkbox__oxdie");
                        String link = input.attr("value");
                        log.debug("		found subcategory {}, {}", e1.text(), link);
                        return new ParsedCategory(null, e1.text(), null, link);
                    }).toList();
            String rss = subCategory.stream().map(rs -> rs.rss()).collect(Collectors.joining(","));
            if (StringUtils.isEmpty(rss)) {
                rss = "all";
            }
            log.debug("found category {}, {}", elemCategory.text(), rss);
            ParsedCategory category = new ParsedCategory(null, elemCategory.text(), null, rss);
            return new AbstractMap.SimpleEntry<>(category, subCategory);
        }).collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue, (k, v) -> {
            throw new IllegalStateException(String.format("Duplicate key %s", k));
        }, LinkedHashMap::new));
     
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.YOUDO;
    }

}
