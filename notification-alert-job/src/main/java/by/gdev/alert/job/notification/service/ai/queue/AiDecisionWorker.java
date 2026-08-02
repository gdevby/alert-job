package by.gdev.alert.job.notification.service.ai.queue;

import by.gdev.alert.job.notification.model.dto.AiNotificationPayload;
import by.gdev.alert.job.notification.model.dto.DecryptedCredential;
import by.gdev.alert.job.notification.service.ai.parser.AutoreplyPlaywrightParser;
import by.gdev.alert.job.notification.service.ai.queue.step.AiStep;
import by.gdev.alert.job.notification.service.ai.queue.step.dto.SendAutoreplyInput;
import by.gdev.alert.job.notification.service.ai.queue.step.dto.StepType;
import by.gdev.common.model.SiteName;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiDecisionWorker {
    private final List<AiStep<?, ?>> steps;

    private Map<StepType, AiStep<?, ?>> stepMap;

    @PostConstruct
    public void init() {
        stepMap = steps.stream()
                .collect(Collectors.toMap(AiStep::type, s -> s));
    }

    private static final List<StepType> PIPELINE = List.of(
            StepType.RESOLVE_SITE,
            StepType.GET_PARSER,
            StepType.GET_CREDENTIALS,
            StepType.SEND_AUTOREPLY,
            StepType.SEND_NOTIFICATION
    );

    void process(AiNotificationPayload payload) {
        log.info("Processing payload for order: {}", payload.getOrder().getLink());
        SiteName site = null;
        AutoreplyPlaywrightParser parser = null;
        DecryptedCredential creds = null;

        for (StepType type : PIPELINE) {
            log.info("АВТООТВЕТ: этап {} -> НАЧАЛО", type);
            switch (type) {
                case RESOLVE_SITE -> {
                    var step = (AiStep<AiNotificationPayload, SiteName>) stepMap.get(type);
                    var r = step.execute(payload);
                    if (r.failed()) return;
                    site = r.value();
                    log.debug("АВТООТВЕТ: этап {} -> сайт определен: {}", type, site);
                }
                case GET_PARSER -> {
                    var step = (AiStep<SiteName, AutoreplyPlaywrightParser>) stepMap.get(type);
                    var r = step.execute(site);
                    if (r.failed()) return;
                    parser = r.value();
                    log.info("АВТООТВЕТ: этап {} -> парсер получен: {}", type, parser.getSiteName());
                }
                case GET_CREDENTIALS -> {
                    var step = (AiStep<AiNotificationPayload, DecryptedCredential>) stepMap.get(type);
                    var r = step.execute(payload);
                    if (r.failed()) return;
                    creds = r.value();
                    log.info("АВТООТВЕТ: этап {} -> учетные данные получены для пользователя: {}", type, creds.login());
                }
                case SEND_AUTOREPLY -> {
                    var step = (AiStep<SendAutoreplyInput, Boolean>) stepMap.get(type);
                    var r = step.execute(new SendAutoreplyInput(parser, creds, payload));
                    if (r.failed()) {
                        log.warn("АВТООТВЕТ: этап {} -> ОШИБКА при отправке автоответа", type);
                    } else {
                        log.info("АВТООТВЕТ: этап {} -> автоответ отправлен успешно", type);
                    }
                }
                case SEND_NOTIFICATION -> {
                    var step = (AiStep<AiNotificationPayload, Void>) stepMap.get(type);
                    step.execute(payload);
                    log.info("АВТООТВЕТ: этап {} -> уведомление отправлено", type);
                }
            }
        }
    }
}