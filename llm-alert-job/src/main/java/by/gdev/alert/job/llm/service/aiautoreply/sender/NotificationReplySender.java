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


@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationReplySender implements ReplySender {

    private final RestTemplate restTemplate;

    @Value("${notification.service.url}")
    private String notificationUrl;

    @Override
    public void send(OrderDTO order, String replyText, AiDecision decision) {
        // пустое
    }

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

