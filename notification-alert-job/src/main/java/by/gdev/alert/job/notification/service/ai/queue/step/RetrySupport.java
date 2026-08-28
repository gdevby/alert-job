package by.gdev.alert.job.notification.service.ai.queue.step;

import by.gdev.alert.job.notification.service.ai.queue.step.dto.StepResult;
import by.gdev.alert.job.notification.service.ai.queue.step.dto.StepType;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class RetrySupport {

    public <T> StepResult<T> retry(StepType stepType, int attempts, long delayMs, Supplier<StepResult<T>> action) {
        StepResult<T> lastResult = null;
        for (int i = 0; i < attempts; i++) {
            StepResult<T> result = action.get();
            if (result.success()) {
                return result;
            }
            lastResult = result;
            if (i < attempts - 1) {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        // Если все попытки неудачны, возвращаем последний результат,
        // иначе создаём fallback-ошибку
        return lastResult != null
                ? lastResult
                : StepResult.fail(stepType, "All retry attempts failed");
    }
}