package by.gdev.alert.job.parser.scheduller;

import by.gdev.alert.job.parser.repository.OrderRepository;
import by.gdev.alert.job.parser.util.cleanup.CleanupProperties;
import by.gdev.alert.job.parser.util.cleanup.RetentionParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataCleanupScheduler {

    private final OrderRepository orderRepository;
    private final CleanupProperties cleanupProperties;
    private final JdbcTemplate jdbcTemplate;

    private static final int BATCH_SIZE = 200;
    private static final int MAX_BATCHES = 5000;
    private static final int BATCH_PAUSE_MS = 30;

    private final AtomicBoolean cleanupInProgress = new AtomicBoolean(false);

    // Очистка при старте приложения
    @EventListener(ApplicationReadyEvent.class)
    public void runOnStartup() {
        log.debug("=== ПРОВЕРКА ЗАПУСКА ОЧИСТКИ ПРИ СТАРТЕ ===");

        String envValue = System.getenv("CLEANUP_RUN_ON_STARTUP");
        boolean shouldRun = "true".equalsIgnoreCase(envValue) || cleanupProperties.isRunstartup();

        if (!shouldRun) {
            log.debug("Очистка при старте ОТКЛЮЧЕНА");
            return;
        }

        log.debug("=== ЗАПУСК ОЧИСТКИ ПРИ СТАРТЕ ПРИЛОЖЕНИЯ ===");
        // Запускаем в отдельном потоке
        Thread cleanupThread = new Thread(() -> {
            try {
                // Даем основному приложению стартовать
                Thread.sleep(2000);

                log.debug("Начинаем очистку в отдельном потоке...");
                run();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Очистка прервана", e);
            } catch (Exception e) {
                log.error("Ошибка очистки", e);
            }
        });

        cleanupThread.setName("cleanup-thread");
        cleanupThread.setDaemon(true); // Демон-поток, чтобы не блокировать остановку приложения
        cleanupThread.start();

        log.debug("Очистка запущена в фоновом потоке, основное приложение продолжает работу");
    }

    @Scheduled(cron = "0 0 3 * * *")  // Каждый день в 03:00:00
    public void run() {
        log.debug("=== Начало очистки старых данных ===");

        if (!cleanupInProgress.compareAndSet(false, true)) {
            log.warn("Очистка уже выполняется, пропускаем запуск");
            return;
        }

        // 1. СОХРАНИТЬ все таймауты ДО очистки
        Map<String, String> originalTimeouts = saveAllTimeouts();

        try {
            setIncreasedTimeouts();
            Duration retention = RetentionParser.parse(cleanupProperties.getRetention());
            log.debug("Период хранения: {} дней", retention.toDays());

            cleanupAllTablesSafely(retention);

        } catch (Exception e) {
            if (e.getCause() != null &&
                    e.getCause().getMessage() != null &&
                    e.getCause().getMessage().contains("Connection is closed")) {

                log.warn("Соединение закрыто после очистки. " +
                        "Это ожидаемо при долгих операциях. " +
                        "Данные вероятно удалены успешно.");

            } else {
                log.error("Настоящая ошибка при очистке: ", e);
            }
        } finally {
            restoreTimeoutsSafely(originalTimeouts);
            cleanupInProgress.set(false);
            log.debug("=== Очистка завершена ===");
        }
    }

    private Map<String, String> saveAllTimeouts() {
        Map<String, String> timeouts = new HashMap<>();
        try {
            jdbcTemplate.query("SHOW SESSION VARIABLES LIKE '%timeout%'", rs -> {
                timeouts.put(rs.getString(1), rs.getString(2));
            });
            log.debug("Сохранили {} таймаутов", timeouts.size());
        } catch (Exception e) {
            log.warn("Не удалось сохранить таймауты: {}", e.getMessage());
        }
        return timeouts;
    }

    private void setIncreasedTimeouts() {
        try {
            jdbcTemplate.execute("SET SESSION wait_timeout = 28800");
            jdbcTemplate.execute("SET SESSION interactive_timeout = 28800");
            jdbcTemplate.execute("SET SESSION net_read_timeout = 7200");
            jdbcTemplate.execute("SET SESSION net_write_timeout = 7200");
            jdbcTemplate.execute("SET SESSION max_statement_time = 0");
            log.debug("Таймауты увеличены для долгой операции");
        } catch (Exception e) {
            log.error("Не удалось установить таймауты: {}", e.getMessage());
            throw e;
        }
    }

    private void restoreTimeoutsSafely(Map<String, String> originalTimeouts) {
        if (originalTimeouts.isEmpty()) return;

        for (Map.Entry<String, String> entry : originalTimeouts.entrySet()) {
            try {
                jdbcTemplate.execute(
                        String.format("SET SESSION %s = %s", entry.getKey(), entry.getValue())
                );
            } catch (Exception e) {
                log.warn("Не удалось восстановить {}: {}", entry.getKey(), e.getMessage());
            }
        }
        log.debug("Таймауты восстановлены");
    }

    private void cleanupAllTablesSafely(Duration retention) {
        Date cutoff = Date.from(
                LocalDateTime.now().minus(retention)
                        .atZone(ZoneId.systemDefault()).toInstant()
        );

        String cutoffHuman = cutoff.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));

        log.debug("Очистка данных старше: {}", cutoffHuman);

        // Используем несколько транзакций вместо одной большой
        try {
            // 1. Очищаем технологии (дочерняя таблица)
            cleanupTechnologiesSafe(cutoff);

            // 2. Очищаем заказы (родительская таблица)
            cleanupOrdersSafe(cutoff);

        } catch (Exception e) {
            log.error("Общая ошибка очистки", e);
        }
    }

    private void cleanupTechnologiesSafe(Date cutoff) {
        log.debug("Шаг 1: Очистка parser_order_technologies...");

        try {
            // Получаем ID заказов для удаления
            String getIdSql = "SELECT DISTINCT pot.parser_order_id " +
                    "FROM parser_order_technologies pot " +
                    "INNER JOIN parser_order po ON pot.parser_order_id = po.id " +
                    "WHERE po.date_time < ? " +
                    "ORDER BY po.date_time ASC " +
                    "LIMIT " + (BATCH_SIZE * MAX_BATCHES);

            List<Long> orderIds = jdbcTemplate.queryForList(getIdSql, Long.class, cutoff);

            if (orderIds.isEmpty()) {
                log.debug("Нет технологий для удаления");
                return;
            }

            log.debug("Найдено заказов с технологиями: {}", orderIds.size());

            // Удаляем технологии пакетами
            int totalDeleted = 0;
            int batchCount = 0;

            for (int i = 0; i < orderIds.size(); i += BATCH_SIZE) {
                batchCount++;
                int toIndex = Math.min(i + BATCH_SIZE, orderIds.size());
                List<Long> batchIds = orderIds.subList(i, toIndex);

                // Создаем строку с ID через запятую
                String idsStr = batchIds.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(","));

                if (!idsStr.isEmpty()) {
                    String deleteSql = "DELETE FROM parser_order_technologies " +
                            "WHERE parser_order_id IN (" + idsStr + ")";

                    int deleted = jdbcTemplate.update(deleteSql);
                    totalDeleted += deleted;

                    //log.debug("Технологии - пакет {}: удалено {} связей, всего {}",
                    //        batchCount, deleted, totalDeleted);

                    // Пауза
                    if (batchCount < orderIds.size() / BATCH_SIZE) {
                        try {
                            Thread.sleep(BATCH_PAUSE_MS);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }

            log.debug("Технологий удалено: {} связей", totalDeleted);

        } catch (Exception e) {
            log.error("Ошибка при очистке технологий", e);
        }
    }

    private void cleanupOrdersSafe(Date cutoff) {
        log.debug("Шаг 2: Очистка parser_order...");

        try {
            long totalToDelete = orderRepository.countByDateTimeBefore(cutoff);

            if (totalToDelete == 0) {
                log.debug("Нет заказов для удаления");
                return;
            }

            log.debug("Найдено заказов для удаления: {}", totalToDelete);

            deleteOrdersInBatches(cutoff, totalToDelete);

        } catch (Exception e) {
            log.error("Ошибка при очистке заказов", e);
        }
    }


    private void deleteOrdersInBatches(Date cutoff, long totalToDelete) throws InterruptedException {
        int batchCount = 0;
        long totalDeleted = 0;
        boolean hasMore = true;

        try {
            while (hasMore && batchCount < MAX_BATCHES) {
                batchCount++;

                int deletedInBatch = orderRepository.deleteBatchByDateTimeBeforeSimple(cutoff, BATCH_SIZE);
                totalDeleted += deletedInBatch;

                //log.debug("Заказы - пакет {}/{}: удалено {}, всего {}",
                //        batchCount, MAX_BATCHES, deletedInBatch, totalDeleted);

                if (deletedInBatch < BATCH_SIZE) {
                    hasMore = false;
                    log.debug("Все заказы удалены");
                }

                // Пауза
                if (hasMore) {
                    Thread.sleep(BATCH_PAUSE_MS);
                }

                // Прогресс
                if (batchCount % 10 == 0) {
                    log.debug("Прогресс: {} пакетов, {} заказов удалено",
                            batchCount, totalDeleted);
                }
            }

            if (batchCount >= MAX_BATCHES && hasMore) {
                log.warn("Достигнут лимит пакетов. Удалено {}/{} заказов",
                        totalDeleted, totalToDelete);
            } else {
                log.debug("Удаление заказов завершено: {} записей", totalDeleted);
            }

        } catch (Exception e) {
            log.error("Ошибка при пакетном удалении заказов", e);
            throw e;
        }
    }
}