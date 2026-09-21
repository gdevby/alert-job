package by.gdev.alert.job.notification.model.dto.credential;

import lombok.Data;

@Data
public class CredentialValidationRequest {
    private Long siteId;
    private String login;
    private String password;
    /** Email пользователя alert-job, передаётся из core для логов Camoufox. */
    private String userEmail;
}