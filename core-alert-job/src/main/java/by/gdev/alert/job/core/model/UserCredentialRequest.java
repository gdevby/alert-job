package by.gdev.alert.job.core.model;

import lombok.Data;

@Data
public class UserCredentialRequest {
    private String userUuid;
    private Long siteId;
    private Long moduleId;
    private String login;
    private String password;
}
