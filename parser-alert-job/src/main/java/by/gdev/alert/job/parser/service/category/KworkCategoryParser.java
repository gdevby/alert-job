package by.gdev.alert.job.parser.service.category;

import by.gdev.alert.job.parser.domain.db.SiteSourceJob;
import by.gdev.alert.job.parser.factory.RestTemplateFactory;
import by.gdev.alert.job.parser.util.SiteName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class KworkCategoryParser implements CategoryParser{

    @Value("${kwork.proxy.active}")
    private boolean kworkProxyActive;

    private final RestTemplateFactory restTemplateFactory;


    @Override
    public Map<ParsedCategory, List<ParsedCategory>> parse(SiteSourceJob siteSourceJob) {
        RestTemplate restTemplate = restTemplateFactory.getRestTemplate(kworkProxyActive);

        ResponseEntity<String> response = restTemplate.getForEntity(siteSourceJob.getParsedURI(), String.class);
        String regex = "\\{\"CATID\":\"([0-9]{1,3})\",\"name\":\"([А-Яа-я\\w\\s\\,]*)\",\"lang\":\"[\\w]{1,2}\",\"short_name\":\"[А-Яа-я\\w\\s\\,]*\","
                + "\"h1\":\"[А-Яа-я\\w\\s\\,]*\",\"seo\":\"[\\-\\w]*\",\"parent\":\"%s\"";
        String body = response.getBody();
        String link = "https://kwork.ru/projects?c=%s";
        Pattern categoryPattern = Pattern.compile(String.format(regex, 0));
        Matcher categoryMatcer = categoryPattern.matcher(body);
        return categoryMatcer.results().map(m -> {
            String cName = m.group(2);
            String cLink = String.format(link, m.group(1));
            log.debug("found category {}, {}", cName, cLink);
            ParsedCategory category = new ParsedCategory(null, cName, null, cLink);
            Pattern subCategoryPattern = Pattern.compile(String.format(regex, m.group(1)));
            Matcher subCategoryMatcher = subCategoryPattern.matcher(body);
            List<ParsedCategory> subList = subCategoryMatcher.results().map(m1 -> {
                String sName = m1.group(2);
                String sLink = String.format(link, m1.group(1));
                log.debug("		found subcategory {}, {}", sName, sLink);
                return new ParsedCategory(null, sName, null, sLink);
            }).toList();
            return new AbstractMap.SimpleEntry<>(category, subList);
        }).collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue, (existing, replacement) -> {
            return existing;
        }, LinkedHashMap::new));
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.KWORK;
    }

}
