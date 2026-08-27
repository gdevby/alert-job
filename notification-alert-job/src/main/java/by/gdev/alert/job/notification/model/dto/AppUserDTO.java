package by.gdev.alert.job.notification.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class AppUserDTO {
    private String uuid;
    private String email;
    private Long telegram;
    private boolean switchOffAlerts;
    private boolean defaultSendType;
    private String country;
}
