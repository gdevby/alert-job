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

    public static CredentialValidationResult success() {
        return new CredentialValidationResult(true, null, null);
    }

    public static CredentialValidationResult fail(String errorMessage) {
        return new CredentialValidationResult(false, errorMessage, null);
    }

    public static CredentialValidationResult fail(String errorCode, String errorMessage) {
        return new CredentialValidationResult(false, errorMessage, errorCode);
    }
}