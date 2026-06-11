package by.gdev.alert.job.core.model.credential.dto;

import lombok.Data;

@Data
public class UserCredentialRequest {
    private String name;
    private String userUuid;
    private Long siteId;
    private Long moduleId;
    private String login;
    private String password;
}
