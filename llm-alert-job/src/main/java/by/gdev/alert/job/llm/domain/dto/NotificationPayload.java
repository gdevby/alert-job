package by.gdev.alert.job.llm.domain.dto;

import by.gdev.alert.job.llm.domain.dto.order.AiAppUserDTO;
import by.gdev.alert.job.llm.domain.dto.order.AiOrderModulesDTO;
import by.gdev.alert.job.llm.domain.dto.order.OrderDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload, который отправляется в Notification Service:
 *  - содержит пользователя, модуль, заказ;
 *  - включает ID учётных данных;
 *  - содержит решение AI по автоответу.
 *
 * Используется для отправки email/telegram уведомлений.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Payload для Notification Service, содержащий заказ, пользователя, модуль и решение AI")
public class NotificationPayload {

    /** Информация о пользователе */
    @Schema(description = "Информация о пользователе", implementation = AiAppUserDTO.class)
    private AiAppUserDTO user;

    /** Информация о модуле */
    @Schema(description = "Модуль, от имени которого пришёл заказ", implementation = AiOrderModulesDTO.class)
    private AiOrderModulesDTO module;

    /** Заказ, который нужно обработать */
    @Schema(description = "Информация о заказе", implementation = OrderDTO.class)
    private OrderDTO order;

    /** ID учётных данных пользователя */
    @Schema(description = "ID учётных данных пользователя", example = "101")
    private Long credentialId;

    /** Решение AI по автоответу */
    @Schema(description = "Решение AI о необходимости ответа", implementation = AiDecision.class)
    private AiDecision decision;
}
