package by.gdev.alert.job.notification.service.ai.queue.step.dto;

public record StepResult<T>(T value, boolean success) {
    public static <T> StepResult<T> ok(T v) { return new StepResult<>(v, true); }
    public static <T> StepResult<T> fail() { return new StepResult<>(null, false); }
    public boolean failed() { return !success; }
}
