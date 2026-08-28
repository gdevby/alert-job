package by.gdev.alert.job.core.model;

import java.util.List;

import lombok.Data;

@Data
public class AppUserDTO {
    private String uuid;
    private String email;
    private Long telegram;
    private boolean switchOffAlerts;
    private boolean defaultSendType;
    private List<UserAlertTimeDTO> alertTimeDTO;
}