package by.gdev.alert.job.parser.service.category;

import by.gdev.alert.job.parser.domain.db.SiteSourceJob;
import by.gdev.alert.job.parser.util.SiteName;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
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
    	Document doc = null;
        try {
        	doc = Jsoup.connect(freelanceRuRss).get();           
        } catch (IOException e) {
        	throw new RuntimeException(e);
        }            
        Elements categoryElements = doc.getElementById("categories").getElementsByClass("col-md-4 col-sm-6 col-xs-12");
        
        if (categoryElements.size() == 0) {
        	throw new RuntimeException("Parent element not found");
        }
        Map<ParsedCategory, List<ParsedCategory>> resultMap = new LinkedHashMap<>();
        resultMap = categoryElements.stream().map(e -> {
        	Element label = e.selectFirst("label");
        	Element input = label.selectFirst("input.spec");
        	ParsedCategory parsedCategory = new ParsedCategory(
                     null,
                     label.text(),
                     Long.valueOf(input.attr(VALUE)),
                     String.format(freelanceRuRssFeed, input.attr(VALUE))
             );
        	log.debug("found category {}", label.text());
        	return parsedCategory;
        }).collect(Collectors.toMap(
                parsedCategory -> parsedCategory,
                parsedCategory -> Collections.emptyList()
        ));           
        return resultMap;      
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.FREELANCERU;
    }
}
