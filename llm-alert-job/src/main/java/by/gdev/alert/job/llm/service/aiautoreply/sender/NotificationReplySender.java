package by.gdev.alert.job.llm.service.aiautoreply.sender;

import by.gdev.alert.job.llm.domain.dto.AiDecision;
import by.gdev.alert.job.llm.domain.dto.NotificationPayload;
import by.gdev.alert.job.llm.domain.dto.order.AiAppUserDTO;
import by.gdev.alert.job.llm.domain.dto.order.AiOrderModulesDTO;
import by.gdev.alert.job.llm.domain.dto.order.OrderDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;


@Component
@RequiredArgsConstructor
public class NotificationReplySender implements ReplySender {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${notification.service.url}")
    private String notificationUrl;

    @Override
    public void send(OrderDTO order, String replyText, AiDecision decision) {
        // пустое
    }

    @Override
    public void sendToNotificationService(
            OrderDTO order,
            AiAppUserDTO user,
            AiOrderModulesDTO module,
            AiDecision decision
    ) {
        NotificationPayload payload = new NotificationPayload(user, module, order, decision);
        //restTemplate.postForEntity(notificationUrl, payload, Void.class);

        Mono.fromCallable(() ->
                        restTemplate.postForEntity(notificationUrl, payload, Void.class)
                )
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
    }
}

