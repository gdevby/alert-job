package by.gdev.alert.job.llm.service.aiautoreply.sender;

import by.gdev.alert.job.llm.domain.dto.AiDecision;
import by.gdev.alert.job.llm.domain.dto.order.AiAppUserDTO;
import by.gdev.alert.job.llm.domain.dto.order.AiOrderModulesDTO;
import by.gdev.alert.job.llm.domain.dto.order.OrderDTO;
import by.gdev.common.model.NotificationTypeEnum;

/**
 * Базовый интерфейс для отправщиков автоответов.
 * <p>
 * Определяет два типа операций:
 * <ul>
 *     <li>{@link #send(OrderDTO, String, AiDecision)} — локальная отправка (например, логирование);</li>
 *     <li>{@link #sendToNotificationService(OrderDTO, AiAppUserDTO, AiOrderModulesDTO, AiDecision, Long)}
 *         — отправка данных в Notification‑сервис.</li>
 * </ul>
 * Реализации интерфейса могут выполнять отправку различными способами:
 * логирование, HTTP‑запросы, интеграции с внешними сервисами и т.д.
 */
public interface ReplySender {

    /**
     * Отправляет автоответ локально (например, в лог или консоль).
     *
     * @param order      заказ
     * @param replyText  текст автоответа
     * @param decision   решение AI
     */
    void send(OrderDTO order, String replyText, AiDecision decision);

    /**
     * Отправляет автоответ в Notification‑сервис.
     *
     * @param order        заказ
     * @param user         пользователь
     * @param module       модуль, от имени которого отправляется ответ
     * @param decision     решение AI
     * @param credentialId ID учётных данных пользователя
     */
    void sendToNotificationService(OrderDTO order, AiAppUserDTO user, AiOrderModulesDTO module, AiDecision decision, Long credentialId, NotificationTypeEnum notificationType);
}
