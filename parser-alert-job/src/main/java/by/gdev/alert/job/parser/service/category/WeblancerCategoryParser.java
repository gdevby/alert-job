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
import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

//@Service
//@Slf4j
public class WeblancerCategoryParser implements CategoryParser{
    @Override
    public Map<ParsedCategory, List<ParsedCategory>> parse(SiteSourceJob siteSourceJob) {    	
        Document doc = null;
        try {
            doc = Jsoup.connect(siteSourceJob.getParsedURI()).get();
        } catch (IOException e) {
        	throw new RuntimeException(e);
        }
            
        Element allCategories = doc.getElementsByClass("category_tree list-unstyled list-wide").get(0);
        return allCategories.children().stream().filter(f -> !f.children().get(0).tagName().equals("b")).map(e -> {
            Elements elements = e.children();
            Element element = elements.get(0);
            ParsedCategory category = new ParsedCategory(null, element.text(), null, null);
            //log.debug("found category {},{}, {}", element.text());
            Element subElements = elements.get(2).getElementsByClass("collapse").get(0);
            Elements subElement = subElements.children();
            List<ParsedCategory> subCategory = subElement.stream().map(sub -> {
                Element n = sub.children().get(0);
                String link = siteSourceJob.getParsedURI() + n.attr("href").replaceAll("/jobs", "");
                //log.debug("		found subcategory {}, {}", n.text(), link);
                return new ParsedCategory(null, n.text(), null, link);
            }).toList();
            return new AbstractMap.SimpleEntry<>(category, subCategory);
        }).collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue, (k, v) -> {
            throw new IllegalStateException(String.format("Duplicate key %s", k));
        }, LinkedHashMap::new));
               
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.WEBLANCER;
    }

}
