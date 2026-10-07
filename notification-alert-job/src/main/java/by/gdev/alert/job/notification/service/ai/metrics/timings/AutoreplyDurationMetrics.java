package by.gdev.alert.job.notification.service.ai.metrics.timings;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Метрика автоответа: длительность операций (Micrometer {@link Timer}).
 * <p>
 * Теги: {@code site}, {@code operation} ({@link AutoreplyMetricOperations}), {@code status} (success/failure).
 * Среднее время: {@code sum / count} у таймера (в Prometheus — {@code _sum} / {@code _count}).
 * Перцентиль 95: client-side quantile {@code 0.95} (в Prometheus — label {@code quantile="0.95"}).
 */
@Slf4j
@Component
public class AutoreplyDurationMetrics {

    /** Имя метрики в Actuator / Prometheus. */
    public static final String METRIC_NAME = "autoreply_operation_duration";

    public static final String STATUS_SUCCESS = "success";
    public static final String STATUS_FAILURE = "failure";

    private final MeterRegistry meterRegistry;
    private final ConcurrentMap<String, Timer> timers = new ConcurrentHashMap<>();

    public AutoreplyDurationMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordDuration(String siteName, String operation, String status, long millis) {
        timerFor(siteName, operation, status).record(millis, TimeUnit.MILLISECONDS);
    }

    /** Замеряет время выполнения и автоматически пишет в метрику. */
    public <T> T time(String siteName, String operation, Supplier<T> action) {
        long start = System.nanoTime();
        boolean ok = false;
        try {
            T result = action.get();
            ok = true;
            return result;
        } finally {
            long ms = (System.nanoTime() - start) / 1_000_000;
            recordDuration(siteName, operation, ok ? STATUS_SUCCESS : STATUS_FAILURE, ms);
        }
    }

    private Timer timerFor(String siteName, String operation, String status) {
        String site = safe(siteName);
        String op = safe(operation);
        String st = safe(status);
        String key = site + "|" + op + "|" + st;
        return timers.computeIfAbsent(key, k ->
                Timer.builder(METRIC_NAME)
                        .description("Метрика автоответа")
                        .tag("site", site)
                        .tag("operation", op)
                        .tag("status", st)
                        .publishPercentiles(0.95)
                        .register(meterRegistry)
        );
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
