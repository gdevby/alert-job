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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PeoplePerHourCategoryParser implements CategoryParser{
    @Override
    public Map<ParsedCategory, List<ParsedCategory>> parse(SiteSourceJob siteSourceJob) {
        String baseURI = siteSourceJob.getParsedURI();

        Document categoriesDocument = getDocument(baseURI);

        Elements elementCategories = categoriesDocument.getElementsByClass("small dropdown__link⤍Megamenu⤚Tqs7l");

        return elementCategories.stream()
                .map(categoryElement -> {
                    String categoryName = categoryElement.text();
                    String link = categoryElement.attr("href");
                    ParsedCategory category = new ParsedCategory(null, categoryName, null, null);
                    log.debug("found category {}", categoryName);
                    List<ParsedCategory> subCategories = Collections.emptyList();
                    if (link.startsWith("/categories")) {
                        Document subCategoriesDocument = getDocument(baseURI + link);
                        Elements elements = subCategoriesDocument.getElementsByClass("link-list__link⤍Menu⤚htHq3");
                        subCategories = elements.stream()
                                .map(Element::text)
                                .map(element -> {
                                    log.debug("found subcategory {}", element);
                                    return new ParsedCategory(null, element, null, null);
                                })
                                .toList();
                    }
                    return new AbstractMap.SimpleEntry<>(category, subCategories);
                }).collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));    }

    @Override
    public SiteName getSiteName() {
        return SiteName.PEOPLEPERHOUR;
    }

    private Document getDocument(String uri) {
        try {
        	return Jsoup.connect(uri).get();
        } catch (IOException e) {
        	throw new RuntimeException(e);
        }
    }

}
