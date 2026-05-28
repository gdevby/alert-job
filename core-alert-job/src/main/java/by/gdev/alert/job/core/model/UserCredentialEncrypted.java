package by.gdev.alert.job.core.model;

import lombok.Data;

@Data
public class UserCredentialEncrypted {
    private String login;
    private String passwordEncrypted;
}
