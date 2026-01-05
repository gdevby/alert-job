package by.gdev.alert.job.parser.scheduller.parser.properties;

import by.gdev.alert.job.parser.util.SiteName;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ParserScheduleConfig {

    private final String PARSER_DELAY_PREFIX = "parser.schedule.sites";
    private final String INITIAL_DELAY_TOKEN = "initial-delay-seconds";
    private final String FIXED_DELAY_TOKEN = "fixed-delay-seconds";
    private final String POINT = ".";

    private final Environment env;

    private String getPropertyInitialDelayName(SiteName siteName){
        return PARSER_DELAY_PREFIX + POINT + siteName.name().toLowerCase() + POINT + INITIAL_DELAY_TOKEN;
    }

    private String getPropertyFixedDelayName(SiteName siteName){
        return PARSER_DELAY_PREFIX + POINT + siteName.name().toLowerCase() + POINT + FIXED_DELAY_TOKEN;
    }

    private long getPropertyValue(String propertyName){
        return Long.parseLong(env.getProperty(propertyName, "0"));
    }

    private ParserScheduleProperties getParserScheduleProperties(SiteName siteName){
        long initialDelay = getPropertyValue(getPropertyInitialDelayName(siteName));
        long fixedDelay = getPropertyValue(getPropertyFixedDelayName(siteName));
        return initialDelay > 0 && fixedDelay > 0 ? new ParserScheduleProperties(initialDelay, fixedDelay) : null;
    }


    public ParserScheduleProperties getForSite(SiteName siteName) {
        return getParserScheduleProperties(siteName);
    }

}

