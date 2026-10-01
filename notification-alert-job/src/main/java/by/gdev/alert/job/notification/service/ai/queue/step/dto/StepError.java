package by.gdev.alert.job.notification.service.ai.queue.step.dto;

public record StepError(String description, byte[] screenshot, String errorCode) {

    public StepError(String description, byte[] screenshot) {
        this(description, screenshot, null);
    }
}