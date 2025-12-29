package by.gdev.alert.job.parser.jobs;

import by.gdev.alert.job.parser.repository.OrderRepository;
import by.gdev.alert.job.parser.util.cleanup.CleanupProperties;
import by.gdev.alert.job.parser.util.cleanup.RetentionParser;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataCleanupJob {

    private final OrderRepository orderRepository;
    private final CleanupProperties cleanupProperties;

    public void cleanupOldOrders(Duration retention) {
        LocalDateTime cutoffLdt = LocalDateTime.now().minus(retention);
        Date cutoff = Date.from(cutoffLdt.atZone(ZoneId.systemDefault()).toInstant());
        long deleted = orderRepository.deleteByDateTimeBefore(cutoff);
        log.info("Очистка завершена. Удалено {} записей старше {}", deleted, cutoff);
    }

    // Каждое воскресенье в 03:00
    @Scheduled(cron = "0 0 3 ? * SUN")
    public void run() {
        Duration retention = RetentionParser.parse(cleanupProperties.getRetention());
        cleanupOldOrders(retention);
    }
}
