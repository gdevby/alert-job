package by.gdev.alert.job.parser.scheduller.parser;

import by.gdev.alert.job.parser.scheduller.SiteParserSchedulerBeanRegistrar;
import by.gdev.alert.job.parser.scheduller.parser.properties.ParserScheduleConfig;
import by.gdev.alert.job.parser.scheduller.parser.properties.ParserScheduleProperties;
import by.gdev.alert.job.parser.service.order.SiteParser;
import by.gdev.alert.job.parser.util.SiteName;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class SchedulerFactory {

    private final List<SiteParser> parsers;
    private final OrderDispatcher dispatcher;
    private final ParserScheduleConfig scheduleConfig;
    private final TaskScheduler taskScheduler;
    private final SiteParserSchedulerBeanRegistrar siteParserSchedulerBeanRegistrar;

    private SiteParser getParserBySiteName(SiteName siteName) {
        return parsers.stream()
                .filter(p -> p.getSiteName() == siteName)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Парсер не найден: " + siteName));
    }

    @PostConstruct
    public void init() {
        registerSchedulers();
    }

    public void registerSchedulers() {
        parsers.forEach(parser -> {
            SiteName siteName = parser.getSiteName();
            ParserScheduleProperties parserScheduleProperties = scheduleConfig.getForSite(siteName);
            if (parserScheduleProperties != null){
                SiteParserScheduler scheduler = new SiteParserScheduler(
                        parser,
                        dispatcher,
                        parserScheduleProperties,
                        taskScheduler
                );

                String beanName = siteName.name().toLowerCase() + SiteParserScheduler.class.getSimpleName();
                siteParserSchedulerBeanRegistrar.registerScheduler(scheduler, beanName);
                log.debug("Зарегистрирован бин '{}' для парсера {}", beanName, siteName);
            }
        });
    }
}


