package by.gdev.alert.job.parser.scheduller;

import by.gdev.alert.job.parser.repository.OrderRepository;
import by.gdev.alert.job.parser.util.cleanup.CleanupProperties;
import by.gdev.alert.job.parser.util.cleanup.RetentionParser;
import org.springframework.transaction.annotation.Transactional;
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
public class DataCleanupScheduler {

    private final OrderRepository orderRepository;
    private final CleanupProperties cleanupProperties;

    @Transactional
    public void cleanupOldOrders(Duration retention) {
        LocalDateTime cutoffLdt = LocalDateTime.now().minus(retention);
        Date cutoff = Date.from(cutoffLdt.atZone(ZoneId.systemDefault()).toInstant());

        long days = retention.toDays();
        long hours = retention.toHoursPart();
        long minutes = retention.toMinutesPart();
        long seconds = retention.toSecondsPart();

        String retentionHuman = String.format("%d дн %d ч %d мин %d сек", days, hours, minutes, seconds);

// cutoff в читаемом формате
        String cutoffHuman = cutoff.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));

        log.debug("Запуск очистки заказов. Порог хранения: {} (удаляем всё до {})",
                retentionHuman, cutoffHuman);


        long deleted = orderRepository.deleteByDateTimeBefore(cutoff);

        if (deleted > 0) {
            log.debug("Очистка завершена успешно. Удалено {} записей старше {}", deleted, cutoff);
        } else {
            log.debug("Очистка завершена. Записей старше {} не найдено", cutoff);
        }
    }

    @Scheduled(cron = "0 0 3 * * *")  // Каждый день в 03:00:00
    public void run() {
        log.debug("Запуск задачи очистки старых заказов...");
        try {
            Duration retention = RetentionParser.parse(cleanupProperties.getRetention());

            long days = retention.toDays();
            long hours = retention.toHoursPart();
            long minutes = retention.toMinutesPart();
            long seconds = retention.toSecondsPart();

            log.debug("Параметры очистки: {} дн {} ч {} мин {} сек", days, hours, minutes, seconds);

            cleanupOldOrders(retention);
        } catch (Exception e) {
            log.error("Ошибка при выполнении задачи очистки", e);
        }
        log.debug("Задача очистки завершена");
    }
}