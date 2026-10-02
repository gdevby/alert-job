package by.gdev.alert.job.core.exeption.ai.credential;

/** Нет jobId (истёк TTL, рестарт core или чужой идентификатор). */
public class CredentialValidationJobNotFoundException extends RuntimeException {
    public CredentialValidationJobNotFoundException(String message) {
        super(message);
    }
}
