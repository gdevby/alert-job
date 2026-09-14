package by.gdev.alert.job.core.exeption;

public class InvalidWebhookUrlException extends RuntimeException {
    public InvalidWebhookUrlException(String message) {
        super(message);
    }
}
