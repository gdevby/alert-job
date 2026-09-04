package by.gdev.alert.job.notification.service.ai.queue.step.impl;

import by.gdev.alert.job.notification.model.AutoreplyMode;
import by.gdev.alert.job.notification.service.ai.queue.step.AiStep;
import by.gdev.alert.job.notification.service.ai.queue.step.RetrySupport;
import by.gdev.alert.job.notification.service.ai.queue.step.dto.SendAutoreplyInput;
import by.gdev.alert.job.notification.service.ai.queue.step.dto.StepResult;
import by.gdev.alert.job.notification.service.ai.queue.step.dto.StepType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SendAutoreplyStep implements AiStep<SendAutoreplyInput, StepResult<Void>> {

    private final RetrySupport retrySupport;

    @Override
    public StepType type() {
        return StepType.SEND_AUTOREPLY;
    }

    @Override
    public StepResult<Void> execute(SendAutoreplyInput input) {
        // Исправленный вызов retry – добавлен StepType.SEND_AUTOREPLY
        return retrySupport.retry(StepType.SEND_AUTOREPLY, 1, 2000, () -> {
            try {
                StepResult<Void> result = input.parser().sendAutoreply(
                        input.creds(),
                        input.payload(),
                        AutoreplyMode.FULL_AUTOREPLY
                );
                if (result.success()) {
                    return StepResult.ok(StepType.SEND_AUTOREPLY, null);
                } else {
                    return StepResult.fail(StepType.SEND_AUTOREPLY, result.getErrorMessage(), result.getScreenshot());
                }
            } catch (Exception e) {
                return StepResult.fail(StepType.SEND_AUTOREPLY, "Исключение при отправке: " + e.getMessage());
            }
        });
    }
}