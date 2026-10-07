package by.gdev.alert.job.notification.service.ai.metrics.errors;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Метрики по ошибкам автоответа.
 * Сюда переехал incrementProblem.
 */
@Slf4j
@Component
public class AutoreplyErrorMetrics {

    private static final String METRIC_NAME = "autoreply_problems";

    private final MeterRegistry meterRegistry;
    private final ConcurrentMap<String, Counter> counters = new ConcurrentHashMap<>();

    public AutoreplyErrorMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void incrementProblem(String siteName, String errorType) {
        String key = siteName + "|" + errorType;
        counters.computeIfAbsent(key, k ->
                Counter.builder(METRIC_NAME)
                        .description("Количество проблемных автооткликов")
                        .tag("site", safe(siteName))
                        .tag("error_type", safe(errorType))
                        .register(meterRegistry)
        ).increment();
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}