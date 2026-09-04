package by.gdev.alert.job.notification.model.dto.credential;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CredentialValidationResult {
    private boolean success;
    private String errorMessage;

    public static CredentialValidationResult success() {
        return new CredentialValidationResult(true, null);
    }

    public static CredentialValidationResult fail(String errorMessage) {
        return new CredentialValidationResult(false, errorMessage);
    }
}