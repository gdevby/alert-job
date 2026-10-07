package by.gdev.alert.job.notification.service.ai.metrics;

import by.gdev.alert.job.notification.service.ai.metrics.errors.AutoreplyErrorMetrics;
import by.gdev.alert.job.notification.service.ai.metrics.timings.AutoreplyDurationMetrics;
import by.gdev.alert.job.notification.service.ai.metrics.timings.AutoreplyTimeouts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Slf4j
@Component
public class AutoreplyMetrics {

    private final AutoreplyErrorMetrics errorMetrics;
    private final AutoreplyDurationMetrics durationMetrics;
    private final AutoreplyTimeouts timeouts;

    public AutoreplyMetrics(AutoreplyErrorMetrics errorMetrics,
                            AutoreplyDurationMetrics durationMetrics,
                            AutoreplyTimeouts timeouts) {
        this.errorMetrics = errorMetrics;
        this.durationMetrics = durationMetrics;
        this.timeouts = timeouts;
    }

    /** Прокидывает ошибку в метрики по ошибкам. */
    public void incrementProblem(String siteName, String errorType) {
        errorMetrics.incrementProblem(siteName, errorType);
    }

    /** Удобный доступ, если нужен напрямую. */
    public AutoreplyErrorMetrics errors() {
        return errorMetrics;
    }

    public void recordDuration(String siteName, String operation, String status, long millis) {
        durationMetrics.recordDuration(siteName, operation, status, millis);
    }

    public <T> T time(String siteName, String operation, Supplier<T> action) {
        return durationMetrics.time(siteName, operation, action);
    }

    public AutoreplyDurationMetrics durations() {
        return durationMetrics;
    }
}