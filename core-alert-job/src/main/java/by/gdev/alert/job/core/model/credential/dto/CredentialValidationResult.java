package by.gdev.alert.job.core.model.credential.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CredentialValidationResult {
    private boolean success;
    private String errorMessage;
    /** Например {@code CREDENTIAL_VALIDATE_CLIENT_TIMEOUT} — обрыв ожидания core→notification. */
    private String errorCode;
    /**
     * Рекомендуемое действие от notification: {@code ROTATE_PROXY}, {@code RETRY_SAME}, {@code ABORT}.
     */
    private String recommendedAction;

    public static CredentialValidationResult success() {
        return new CredentialValidationResult(true, null, null, null);
    }

    public static CredentialValidationResult fail(String errorMessage) {
        return new CredentialValidationResult(false, errorMessage, null, null);
    }

    public static CredentialValidationResult fail(String errorCode, String errorMessage) {
        return new CredentialValidationResult(false, errorMessage, errorCode, null);
    }

    public static CredentialValidationResult fail(String errorCode, String errorMessage, String recommendedAction) {
        return new CredentialValidationResult(false, errorMessage, errorCode, recommendedAction);
    }
}
