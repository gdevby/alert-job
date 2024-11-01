package by.gdev.alert.job.parser.service.category;

import by.gdev.alert.job.parser.domain.db.SiteSourceJob;
import by.gdev.alert.job.parser.factory.RestTemplateFactory;
import by.gdev.alert.job.parser.util.SiteName;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@Slf4j
@RequiredArgsConstructor
public class FreelancehuntCategoryParser implements CategoryParser {

    @Value("${freelancehunt.proxy.active}")
    private boolean freelancehuntProxyActive;
    private final RestTemplateFactory restTemplateFactory;

    @Override
    public Map<ParsedCategory, List<ParsedCategory>> parse(SiteSourceJob siteSourceJob) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("user-agent", "Application");
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        RestTemplate restTemplate = restTemplateFactory.getRestTemplate(freelancehuntProxyActive);

        ResponseEntity<String> res = restTemplate.exchange(siteSourceJob.getParsedURI(), HttpMethod.GET, entity,
                String.class);
        Document doc = Jsoup.parse(res.getBody());
        Element allCategories = doc.getElementById("skill-group-selector");
        Elements el = allCategories.children().select("div.panel.panel-default");
        return el.stream().map(e -> {
            Element elemCategory = e.selectFirst("div.panel-heading");
            ParsedCategory category = new ParsedCategory(null, elemCategory.text(), null, null);
            Element elemSubCategory = e.selectFirst("ul.panel-body.collapse");
            List<ParsedCategory> subCategory = elemSubCategory.children().select("li.accordion-inner.clearfix").stream()
                    .map(sub -> {
                        // remove first element (orders count)
                        List<String> listText = Lists.newArrayList(sub.text().split(" "));
                        listText.remove(0);
                        String text = listText.stream().collect(Collectors.joining(" "));
                        String link = sub.select("a").get(0).attr("href");
                        log.debug("		found subcategory {}, {}", text, link);
                        return new ParsedCategory(null, text, null, link);
                    }).toList();
            return new AbstractMap.SimpleEntry<>(category, subCategory);
        }).collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue, (k, v) -> {
            throw new IllegalStateException(String.format("Duplicate key %s", k));
        }, LinkedHashMap::new));
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.FREELANCEHUNT;
    }

}
