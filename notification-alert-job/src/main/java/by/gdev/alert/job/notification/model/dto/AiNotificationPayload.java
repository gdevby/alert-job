package by.gdev.alert.job.notification.model.dto;

import by.gdev.common.model.NotificationTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiNotificationPayload {

    private AiAppUserDTO user;
    private AiOrderModulesDTO module;
    private OrderDTO order;
    private Long credentialId;
    private AiDecision decision;
    private NotificationTypeEnum notificationType;
}
