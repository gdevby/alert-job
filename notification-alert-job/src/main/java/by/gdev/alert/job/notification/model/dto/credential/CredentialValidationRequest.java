package by.gdev.alert.job.notification.model.dto.credential;

import lombok.Data;

@Data
public class CredentialValidationRequest {
    private Long siteId;
    private String login;
    private String password;
}