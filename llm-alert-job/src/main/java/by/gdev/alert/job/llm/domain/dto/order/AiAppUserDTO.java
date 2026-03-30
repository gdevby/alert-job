package by.gdev.alert.job.llm.domain.dto.order;

import lombok.Data;

@Data
public class AiAppUserDTO {
    private String email;
    private Long telegram;
    private boolean switchOffAlerts;
    private boolean defaultSendType;
}
