package by.gdev.alert.job.notification.service.ai.queue.step.impl;

import by.gdev.alert.job.notification.service.ai.queue.step.*;
import by.gdev.alert.job.notification.service.ai.queue.step.dto.SendAutoreplyInput;
import by.gdev.alert.job.notification.service.ai.queue.step.dto.StepResult;
import by.gdev.alert.job.notification.service.ai.queue.step.dto.StepType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SendAutoreplyStep implements AiStep<SendAutoreplyInput, Boolean> {

    private final RetrySupport retrySupport;

    @Override
    public StepType type() {
        return StepType.SEND_AUTOREPLY;
    }

    @Override
    public StepResult<Boolean> execute(SendAutoreplyInput input) {
        return retrySupport.retry(3, 2000, () -> {
            try {
                boolean ok = input.parser().sendAutoreply(
                        input.creds(),
                        input.payload()
                );
                return ok ? StepResult.ok(true) : StepResult.fail();
            } catch (Exception e) {
                return StepResult.fail();
            }
        });
    }
}



