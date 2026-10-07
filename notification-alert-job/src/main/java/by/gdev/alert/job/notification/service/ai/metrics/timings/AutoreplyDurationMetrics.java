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
 * Метрики времени выполнения операций автоответа.
 */
@Slf4j
@Component
public class AutoreplyDurationMetrics {

    private static final String METRIC_NAME = "autoreply_operation_duration";

    private final MeterRegistry meterRegistry;
    private final ConcurrentMap<String, Timer> timers = new ConcurrentHashMap<>();

    public AutoreplyDurationMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordDuration(String siteName, String operation, String status, long millis) {
        String key = siteName + "|" + operation + "|" + status;
        timers.computeIfAbsent(key, k ->
                Timer.builder(METRIC_NAME)
                        .description("Время выполнения операций автоответа")
                        .tag("site", safe(siteName))
                        .tag("operation", safe(operation))
                        .tag("status", safe(status))
                        .publishPercentiles(0.5, 0.95, 0.99)
                        .register(meterRegistry)
        ).record(millis, TimeUnit.MILLISECONDS);
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
            recordDuration(siteName, operation, ok ? "success" : "failure", ms);
        }
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}