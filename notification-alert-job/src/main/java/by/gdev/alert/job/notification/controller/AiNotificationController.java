package by.gdev.alert.job.notification.controller;

import by.gdev.alert.job.notification.model.dto.*;
import by.gdev.alert.job.notification.service.MailService;
import by.gdev.alert.job.notification.service.ai.credential.UserCredentialService;
import by.gdev.alert.job.notification.service.ai.parser.AutoreplyParserFactory;
import by.gdev.alert.job.notification.service.ai.parser.AutoreplyPlaywrightParser;
import by.gdev.common.model.NotificationType;
import by.gdev.common.model.SiteName;
import by.gdev.common.model.UserNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notification/api/ai")
@Slf4j
@RequiredArgsConstructor
public class AiNotificationController {
    private final MailService service;
    private final UserCredentialService userCredentialService;
    private final AutoreplyParserFactory parserFactory;

    @PostMapping("/decision")
    public ResponseEntity<Void> receiveAiDecision(@RequestBody AiNotificationPayload payload) {
        log.info("AI REPLY = {}", payload.getDecision().reply());
        AiAppUserDTO user = payload.getUser();
        if (user != null){
            boolean isDefaultSendType = user.isDefaultSendType();
            DecryptedCredential credential = userCredentialService.getUserCredentials(payload);

            if (user.getEmail()!= null || user.getTelegram() != null){
                UserNotification userNotification = new UserNotification();
                userNotification.setType(NotificationType.AUTO_REPLY);
                if (isDefaultSendType) {
                    // EMAIL → HTML шаблон
                    String html = buildAiReplyEmailTemplate(payload);
                    userNotification.setMessage(html);
                    userNotification.setToMail(user.getEmail());
                    log.debug("AI нотификация по почте");
                    service.sendMessage(userNotification).subscribe();
                }
                else {
                    // TELEGRAM → обычный текст
                    userNotification.setMessage(payload.getDecision().reply());
                    userNotification.setToMail(user.getTelegram().toString());
                    log.debug("AI нотификация по телеграм");
                    service.sendMessageToTelegram(userNotification);
                }
            }
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/testlogin")
    public Mono<ResponseEntity<?>> receiveTestCase(
            @RequestParam String uuid,
            @RequestParam String site
    ) {
        AiNotificationPayload payload = buildTestAiDecision(uuid);

        // Проверяем enum
        SiteName siteEnum;
        try {
            siteEnum = SiteName.valueOf(site.toUpperCase());
        } catch (Exception e) {
            return Mono.just(
                    ResponseEntity.badRequest().body(
                            Map.of("error", "Unknown site: " + site)
                    )
            );
        }

        // Проверяем наличие парсера
        AutoreplyPlaywrightParser parser;
        try {
            parser = parserFactory.getParser(siteEnum);
        } catch (IllegalArgumentException e) {
            return Mono.just(
                    ResponseEntity.badRequest().body(
                            Map.of("error", "Parser not found for site: " + siteEnum)
                    )
            );
        }

        // Основная логика
        return userCredentialService.getMonoUserCredentials(payload)
                .flatMap(credential -> Mono.fromCallable(() -> {
                                    boolean ok = parser.sendAutoreply(credential, payload);
                                    log.info("Parser result = {}", ok);
                                    return ok;
                                })
                                .subscribeOn(Schedulers.boundedElastic())
                                .map(ok -> ResponseEntity.ok().build())
                );
    }


    private AiNotificationPayload buildTestAiDecision(String uid){
        AiNotificationPayload payload = new AiNotificationPayload();
        AiAppUserDTO user = new AiAppUserDTO();
        user.setUuid(uid);
        user.setEmail("provodnik_new@mail.ru");
        user.setDefaultSendType(true);
        payload.setUser(user);

        AiOrderModulesDTO module = new AiOrderModulesDTO();
        module.setId(16L);
        module.setName("weblancer");
        payload.setModule(module);

        OrderDTO order = new OrderDTO();
        SourceSiteDTO site = new SourceSiteDTO();
        site.setId(4L);
        order.setSourceSite(site);
        order.setLink("https://www.weblancer.net/freelance/sluzhba-podderzhki-56/administrator-onlain-platformi-udalyonno-1265808/");

        AiDecision decision = new AiDecision(
                true,
                0.92,
                "Совпадение по ключевым словам",
                "Готов выполнить задачу!",
                List.of("java", "spring"),
                List.of("docker"),
                "Категория совпала",
                "Подкатегория совпала"
        );
        payload.setDecision(decision);
        payload.setOrder(order);

        return payload;
    }


    private String buildAiReplyEmailTemplate(AiNotificationPayload payload) {

        String replyHtml = payload.getDecision().reply()
                .replace("\n", "<br>");

        return String.format("""
        <div style="font-family: Arial, sans-serif; padding: 12px; border: 1px solid #e5e5e5; border-radius: 8px; background: #fafafa; margin-bottom: 12px;">
            <h3 style="margin: 0 0 10px 0; color: #333;">Автоответ от AI</h3>

            <p style="margin: 4px 0;">
                <strong>Модуль:</strong> %s
            </p>

            <p style="margin: 4px 0;">
                <strong>Название заказа:</strong> %s
            </p>

            <p style="margin: 4px 0;">
                <strong>Ссылка:</strong>
                <a href="%s" style="color: #1a73e8;">%s</a>
            </p>

            <hr style="margin: 12px 0; border: none; border-top: 1px solid #ddd;">

            <p style="margin: 4px 0;">
                <strong>Ответ AI:</strong>
            </p>

            <div style="padding: 10px; background: #fff; border: 1px solid #ddd; border-radius: 6px;">
                %s
            </div>
        </div>
        """,
                payload.getModule().getName(),
                payload.getOrder().getTitle(),
                payload.getOrder().getLink(),
                payload.getOrder().getLink(),
                replyHtml
        );
    }
}
