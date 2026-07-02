package by.gdev.alert.job.llm.service.aiautoreply.sender;

import by.gdev.alert.job.llm.domain.dto.AiDecision;
import by.gdev.alert.job.llm.domain.dto.NotificationPayload;
import by.gdev.alert.job.llm.domain.dto.order.AiAppUserDTO;
import by.gdev.alert.job.llm.domain.dto.order.AiOrderModulesDTO;
import by.gdev.alert.job.llm.domain.dto.order.OrderDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;


/**
 * Отправщик автоответов в Notification‑сервис.
 * <p>
 * Формирует {@link NotificationPayload} и отправляет его через HTTP POST
 * в асинхронном режиме с использованием Reactor.
 * <p>
 * Используется в рабочем режиме для реальной доставки автоответов
 * пользователям через внешний Notification‑модуль.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationReplySender implements ReplySender {

    private final RestTemplate restTemplate;

    @Value("${notification.service.url}")
    private String notificationUrl;

    /**
     * Метод не используется для Notification‑сервиса.
     * Реальная отправка выполняется через {@link #sendToNotificationService}.
     */
    @Override
    public void send(OrderDTO order, String replyText, AiDecision decision) {
        // пустое
    }

    /**
     * Отправляет данные автоответа в Notification‑сервис.
     * <p>
     * Логика:
     * <ul>
     *     <li>формирует {@link NotificationPayload};</li>
     *     <li>логирует параметры отправки;</li>
     *     <li>выполняет POST‑запрос асинхронно через Reactor;</li>
     *     <li>использует boundedElastic для неблокирующего вызова RestTemplate.</li>
     * </ul>
     *
     * @param order        заказ
     * @param user         пользователь
     * @param module       модуль, от имени которого отправляется ответ
     * @param decision     решение AI
     * @param credentialId ID учётных данных пользователя
     */
    @Override
    public void sendToNotificationService(OrderDTO order, AiAppUserDTO user, AiOrderModulesDTO module, AiDecision decision, Long credentialId) {
        NotificationPayload payload = new NotificationPayload(user, module, order, credentialId, decision);

        log.debug("NOTIFICATION → отправка запроса: url={}, user={}, module={}, orderId={}, credentialId={}",
                notificationUrl,
                user.getEmail(),
                module.getName(),
                order.getMessage(),
                credentialId
        );

        Mono.fromCallable(() ->
                        restTemplate.postForEntity(notificationUrl, payload, Void.class)
                )
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
    }
}
