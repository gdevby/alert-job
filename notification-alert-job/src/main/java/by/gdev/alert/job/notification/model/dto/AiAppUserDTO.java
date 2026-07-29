package by.gdev.alert.job.notification.model.dto;

import lombok.Data;

@Data
public class AiAppUserDTO {
    private String uuid;
    private String email;
    private Long telegram;
    private boolean switchOffAlerts;
    private boolean defaultSendType;
}
