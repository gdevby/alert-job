package by.gdev.alert.job.notification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Отдельный пул для @Scheduled: длинная проверка прокси не блокирует опрос IMAP.
 */
@Configuration
public class NotificationSchedulingConfig {

    @Bean
    ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("notification-sched-");
        scheduler.initialize();
        return scheduler;
    }
}
