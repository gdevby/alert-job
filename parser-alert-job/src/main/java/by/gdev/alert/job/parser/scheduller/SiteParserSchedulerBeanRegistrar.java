package by.gdev.alert.job.parser.scheduller;

import by.gdev.alert.job.parser.scheduller.parser.SiteParserScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SiteParserSchedulerBeanRegistrar {

    private final GenericApplicationContext context;

    public void registerScheduler(SiteParserScheduler scheduler, String beanName) {
        context.registerBean(beanName, SiteParserScheduler.class, () -> scheduler);
    }
}

