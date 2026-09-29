package by.gdev.alert.job.notification.service.ai.queue.step.dto;

public record StepResult<T>(StepType step, T value, boolean success, StepError error) {

    public static <T> StepResult<T> ok(StepType step, T value) {
        return new StepResult<>(step, value, true, null);
    }

    public static <T> StepResult<T> fail(StepType step, String description, byte[] screenshot) {
        return new StepResult<>(step, null, false, new StepError(description, screenshot));
    }

    public static <T> StepResult<T> fail(StepType step, String errorCode, String description, byte[] screenshot) {
        return new StepResult<>(step, null, false, new StepError(description, screenshot, errorCode));
    }

    public static <T> StepResult<T> fail(StepType step, String description) {
        return fail(step, description, (byte[]) null);
    }

    public static <T> StepResult<T> fail(StepType step, String errorCode, String description) {
        return fail(step, errorCode, description, (byte[]) null);
    }

    public boolean failed() {
        return !success;
    }

    public String getErrorMessage() {
        return error != null ? error.description() : null;
    }

    public String getErrorCode() {
        return error != null ? error.errorCode() : null;
    }

    public byte[] getScreenshot() {
        return error != null ? error.screenshot() : null;
    }

    public String getStepDisplayName() {
        return step != null ? step.getDisplayName() : "UNKNOWN";
    }
}