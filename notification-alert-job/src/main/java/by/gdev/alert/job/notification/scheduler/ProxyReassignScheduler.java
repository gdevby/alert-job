package by.gdev.alert.job.notification.scheduler;

import by.gdev.alert.job.notification.service.ai.proxy.AssignedProxyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProxyReassignScheduler {

    private final AssignedProxyService assignedProxyService;

    @Value("${proxy.reassign.interval.minutes:30}")
    private int intervalMinutes;

    @Scheduled(fixedDelayString = "#{${proxy.reassign.interval.minutes:30} * 60 * 1000}")
    public void reassignProxies() {
        log.debug("Перераспределение прокси для пользователей (интервал {} мин)", intervalMinutes);
        assignedProxyService.reassignProxies();
    }
}
