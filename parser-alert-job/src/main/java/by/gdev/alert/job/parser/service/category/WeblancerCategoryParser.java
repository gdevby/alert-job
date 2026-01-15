package by.gdev.alert.job.parser.service.category;

import by.gdev.alert.job.parser.domain.db.SiteSourceJob;
import by.gdev.alert.job.parser.util.SiteName;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class WeblancerCategoryParser implements CategoryParser{

    @Override
    public Map<ParsedCategory, List<ParsedCategory>> parse(SiteSourceJob siteSourceJob) {

        Document doc;
        try {
            doc = Jsoup.connect(siteSourceJob.getParsedURI()).get();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Map<ParsedCategory, List<ParsedCategory>> result = new LinkedHashMap<>();

        for (Element h3 : doc.select("h3.mb-4.text-xl.font-semibold")) {

            String title = h3.text().trim();
            //log.info("Root category: site name -  {}, category name - {}", getSiteName().name(), title);
            Element container = h3.parent().selectFirst("div.flex.flex-wrap.gap-2");
            if (container == null) continue;

            List<ParsedCategory> subcategories = container.select("a").stream()
                    .map(a -> {
                        String link = resolveLink(siteSourceJob, a.attr("href"));
                        //log.info("Parsing category: site name -  {}, category name - {}, link - {}", getSiteName().name(), a.text(), link);
                        return new ParsedCategory(null, a.text(), null, link);
                    })
                    .toList();

            ParsedCategory root = new ParsedCategory(null, title, null, null);
            result.put(root, subcategories);
        }

        return result;
    }

    private String resolveLink(SiteSourceJob job, String href) {
        URI base = URI.create(job.getParsedURI());
        URI resolved = base.resolve(href);
        return resolved.toString();
    }

    @Override
    public SiteName getSiteName() {
        return SiteName.WEBLANCER;
    }
}
