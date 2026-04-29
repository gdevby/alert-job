package by.gdev.alert.job.parser.service.category.check;

import by.gdev.alert.job.parser.domain.db.SiteSourceJob;
import by.gdev.alert.job.parser.service.category.CategoryParser;
import by.gdev.alert.job.parser.util.SiteName;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CategoryParserFactory {

    private final SiteNameResolver resolver;
    private final Map<SiteName, CategoryParser> registry = new HashMap<>();

    public CategoryParserFactory(List<CategoryParser> parsers, SiteNameResolver resolver) {
        this.resolver = resolver;
        for (CategoryParser parser : parsers) {
            registry.put(parser.getSiteName(), parser);
        }
    }

    public CategoryParser getParser(SiteSourceJob job) {
        SiteName siteName = resolver.resolve(job.getName());
        CategoryParser parser = registry.get(siteName);
        if (parser == null) {
            throw new IllegalArgumentException("Parser not found for site: " + job.getName());
        }
        return parser;
    }
}



