package by.gdev.alert.job.notification.service.ai.queue.step;

import by.gdev.alert.job.notification.service.ai.queue.step.dto.StepResult;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class RetrySupport {

    public <T> StepResult<T> retry(int attempts, long delayMs, Supplier<StepResult<T>> action) {
        for (int i = 1; i <= attempts; i++) {
            StepResult<T> result = action.get();
            if (result.success()) {
                return result;
            }

            if (i < attempts) {
                try { Thread.sleep(delayMs); } catch (InterruptedException ignored) {}
            }
        }
        return StepResult.fail();
    }
}

