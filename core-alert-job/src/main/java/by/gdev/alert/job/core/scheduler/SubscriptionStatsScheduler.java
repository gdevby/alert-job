package by.gdev.alert.job.core.scheduler;

import by.gdev.alert.job.core.service.SubscriptionStatisticsService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class SubscriptionStatsScheduler {

    private final SubscriptionStatisticsService statsService;

    @PostConstruct
    public void init() {
        log.info("Сбор статистики подписок при старте приложения (за сегодня)");
        statsService.collectTodayStat();
    }

    @Scheduled(cron = "0 0 1 * * ?")
    public void collectStatsDaily() {
        log.info("Запуск сбора статистики подписок (ежедневно в 1:00)...");
        statsService.collectTodayStat();
    }
}