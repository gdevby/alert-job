package by.gdev.alert.job.parser.configuration;

import by.gdev.alert.job.parser.service.category.CategoryParser;
import by.gdev.alert.job.parser.util.SiteName;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
public class CategoryParserConfiguration {

    @Bean
    public Map<SiteName, CategoryParser> categoryParserMap(List<CategoryParser> categoryParsers) {
        return categoryParsers.stream()
                .collect(
                        Collectors.toMap(
                                CategoryParser::getSiteName,
                                categoryParser -> categoryParser
                        )
                );
    }

}
