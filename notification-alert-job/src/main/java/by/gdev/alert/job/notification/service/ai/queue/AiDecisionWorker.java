package by.gdev.alert.job.notification.service.ai.queue;

import by.gdev.alert.job.notification.model.dto.AiNotificationPayload;
import by.gdev.alert.job.notification.model.dto.DecryptedCredential;
import by.gdev.alert.job.notification.service.ai.parser.AutoreplyPlaywrightParser;
import by.gdev.alert.job.notification.service.ai.queue.step.AiStep;
import by.gdev.alert.job.notification.service.ai.queue.step.dto.SendAutoreplyInput;
import by.gdev.alert.job.notification.service.ai.queue.step.dto.StepResult;
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

    public void process(AiNotificationPayload payload) {
        log.info("Processing payload for order: {}", payload.getOrder().getLink());
        SiteName site = null;
        AutoreplyPlaywrightParser parser = null;
        DecryptedCredential creds = null;

        StepResult<?> lastError = null;

        for (StepType type : PIPELINE) {
            String userUuid = payload.getUser() != null ? payload.getUser().getUuid() : "unknown";
            log.info("АВТООТВЕТ: этап {} -> НАЧАЛО", type);

            if (lastError != null && type != StepType.SEND_NOTIFICATION) {
                log.info("АВТООТВЕТ: этап {} -> пропускаем из-за предыдущей ошибки", type);
                continue;
            }

            switch (type) {
                case RESOLVE_SITE -> {
                    var step = (AiStep<AiNotificationPayload, StepResult<SiteName>>) stepMap.get(type);
                    var r = step.execute(payload);
                    if (r.failed()) {
                        log.warn("АВТООТВЕТ: этап {} -> ОШИБКА, пользователь: {}", type, userUuid);
                        lastError = r;
                    } else {
                        site = r.value();
                        log.debug("АВТООТВЕТ: этап {} -> сайт определен: {}, пользователь: {}", type, site, userUuid);
                    }
                }
                case GET_PARSER -> {
                    if (site == null) {
                        log.warn("АВТООТВЕТ: этап {} -> сайт не определён, пропускаем", type);
                        lastError = StepResult.fail(StepType.GET_PARSER, "Сайт не определён");
                        continue;
                    }
                    var step = (AiStep<SiteName, StepResult<AutoreplyPlaywrightParser>>) stepMap.get(type);
                    var r = step.execute(site);
                    if (r.failed()) {
                        log.warn("АВТООТВЕТ: этап {} -> ОШИБКА, пользователь: {}", type, userUuid);
                        lastError = r;
                    } else {
                        parser = r.value();
                        log.info("АВТООТВЕТ: этап {} -> парсер получен: {}, пользователь: {}", type, parser.getSiteName(), userUuid);
                    }
                }
                case GET_CREDENTIALS -> {
                    if (payload == null) {
                        log.warn("АВТООТВЕТ: этап {} -> payload null, пропускаем", type);
                        lastError = StepResult.fail(StepType.GET_CREDENTIALS, "Payload null");
                        continue;
                    }
                    var step = (AiStep<AiNotificationPayload, StepResult<DecryptedCredential>>) stepMap.get(type);
                    var r = step.execute(payload);
                    if (r.failed()) {
                        log.warn("АВТООТВЕТ: этап {} -> ОШИБКА, пользователь: {}", type, userUuid);
                        lastError = r;
                    } else {
                        creds = r.value();
                        log.info("АВТООТВЕТ: этап {} -> учетные данные получены для пользователя: {}, пользователь: {}", type, creds.login(), userUuid);
                    }
                }
                case SEND_AUTOREPLY -> {
                    if (parser == null || creds == null) {
                        log.warn("АВТООТВЕТ: этап {} -> парсер или учетные данные отсутствуют, пропускаем", type);
                        lastError = StepResult.fail(StepType.SEND_AUTOREPLY, "Парсер или учетные данные отсутствуют");
                        continue;
                    }
                    var step = (AiStep<SendAutoreplyInput, StepResult<Void>>) stepMap.get(type);
                    var r = step.execute(new SendAutoreplyInput(parser, creds, payload));
                    if (r.failed()) {
                        log.warn("АВТООТВЕТ: этап {} -> ОШИБКА при отправке автоответа, пользователь: {}", type, userUuid);
                        lastError = r;
                    } else {
                        log.info("АВТООТВЕТ: этап {} -> автоответ отправлен успешно, пользователь: {}", type, userUuid);
                    }
                }
                case SEND_NOTIFICATION -> {
                    payload.setStepResult(lastError);
                    var step = (AiStep<AiNotificationPayload, StepResult<Void>>) stepMap.get(type);
                    step.execute(payload);
                    log.info("АВТООТВЕТ: этап {} -> уведомление отправлено, пользователь: {}", type, userUuid);
                    return;
                }
            }
        }
    }
}