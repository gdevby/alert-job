package by.gdev.alert.job.core.model.credential.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CredentialValidationJobResponse {
    private String jobId;
    private CredentialValidationJobStatus status;
    /**
     * Заполняется при status {@link CredentialValidationJobStatus#COMPLETED} или {@link CredentialValidationJobStatus#FAILED}.
     */
    private CredentialValidationResult result;
}
