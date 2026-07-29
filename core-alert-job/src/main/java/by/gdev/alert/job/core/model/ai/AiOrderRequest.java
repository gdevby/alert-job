package by.gdev.alert.job.core.model.ai;

import by.gdev.common.model.NotificationTypeEnum;
import by.gdev.common.model.OrderDTO;
import lombok.Data;

import java.util.List;

@Data
public class AiOrderRequest {
    private AiAppUserDTO user;
    private AiOrderModulesDTO module;
    private Long credentialId;
    private Long templateId;
    private Long promtId;
    private List<OrderDTO> orders;
    private NotificationTypeEnum notificationType;
}

