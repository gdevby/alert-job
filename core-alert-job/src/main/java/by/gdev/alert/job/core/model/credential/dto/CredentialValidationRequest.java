package by.gdev.alert.job.core.model.credential.dto;

import lombok.Data;

@Data
public class CredentialValidationRequest {
    private Long siteId;
    private String login;
    private String password;
}