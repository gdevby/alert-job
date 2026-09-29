package by.gdev.alert.job.notification.model.dto.credential;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CredentialValidationResult {
    private boolean success;
    private String errorMessage;
    /** Машиночитаемый код (например {@code FLRU_VALIDATE_HCAPTCHA_TWO_CAPTCHA}). */
    private String errorCode;
    /**
     * Рекомендуемое действие: {@code ROTATE_PROXY}, {@code RETRY_SAME}, {@code ABORT}.
     * Заполняется при ошибке.
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
