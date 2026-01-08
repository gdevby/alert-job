package by.gdev.alert.job.parser.scheduller.parser.properties;

import by.gdev.alert.job.parser.util.SiteName;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ParserScheduleConfig {

    private static final long INITIAL_DELAY_DEFAULT =300;
    private static final long FIXED_DELAY_DEFAULT = 600;

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
        return env.getProperty(propertyName, Long.class, -1L);
    }

    private ParserScheduleProperties getParserScheduleProperties(SiteName siteName){
        long initialDelay = getPropertyValue(getPropertyInitialDelayName(siteName)) > 0
                ? getPropertyValue(getPropertyInitialDelayName(siteName)) : INITIAL_DELAY_DEFAULT;
        long fixedDelay = getPropertyValue(getPropertyFixedDelayName(siteName)) > 0
                ? getPropertyValue(getPropertyFixedDelayName(siteName)) : FIXED_DELAY_DEFAULT;
        return new ParserScheduleProperties(initialDelay, fixedDelay);
    }


    public ParserScheduleProperties getForSite(SiteName siteName) {
        return getParserScheduleProperties(siteName);
    }

}

