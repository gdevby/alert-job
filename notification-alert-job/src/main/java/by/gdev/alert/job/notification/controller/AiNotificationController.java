package by.gdev.alert.job.notification.controller;

import by.gdev.alert.job.notification.model.dto.AiAppUserDTO;
import by.gdev.alert.job.notification.model.dto.AiNotificationPayload;
import by.gdev.alert.job.notification.service.MailService;
import by.gdev.common.model.NotificationType;
import by.gdev.common.model.UserNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notification/api/ai")
@Slf4j
@RequiredArgsConstructor
public class AiNotificationController {
    private final MailService service;

    @PostMapping("/decision")
    public ResponseEntity<Void> receiveAiDecision(@RequestBody AiNotificationPayload payload) {
        log.info("AI REPLY = {}", payload.getDecision().reply());
        AiAppUserDTO user = payload.getUser();
        if (user != null){
            boolean isDefaultSendType = user.isDefaultSendType();

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
