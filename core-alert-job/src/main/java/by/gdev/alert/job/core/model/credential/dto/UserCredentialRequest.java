package by.gdev.alert.job.core.model.credential.dto;

import lombok.Data;

@Data
public class UserCredentialRequest {
    private String name;
    private Long siteId;
    private String login;
    private String password;
}
