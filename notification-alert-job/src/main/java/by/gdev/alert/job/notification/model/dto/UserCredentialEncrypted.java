package by.gdev.alert.job.notification.model.dto;

import lombok.Data;

@Data
public class UserCredentialEncrypted {
    private String login;
    private String passwordEncrypted;
}
