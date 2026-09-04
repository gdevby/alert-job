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

    public static CredentialValidationResult success() {
        return new CredentialValidationResult(true, null);
    }

    public static CredentialValidationResult fail(String errorMessage) {
        return new CredentialValidationResult(false, errorMessage);
    }
}