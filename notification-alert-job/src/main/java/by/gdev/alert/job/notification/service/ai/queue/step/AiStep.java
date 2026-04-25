package by.gdev.alert.job.notification.service.ai.queue.step;

import by.gdev.alert.job.notification.service.ai.queue.step.dto.StepResult;
import by.gdev.alert.job.notification.service.ai.queue.step.dto.StepType;

public interface AiStep<I, O> {
    StepType type();
    StepResult<O> execute(I input);
}


