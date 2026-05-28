package by.gdev.alert.job.notification.service.ai.queue.step.impl;

import by.gdev.alert.job.notification.model.dto.AiNotificationPayload;
import by.gdev.alert.job.notification.service.MailService;
import by.gdev.alert.job.notification.service.ai.queue.step.AiStep;
import by.gdev.alert.job.notification.service.ai.queue.step.RetrySupport;
import by.gdev.alert.job.notification.service.ai.queue.step.dto.StepResult;
import by.gdev.alert.job.notification.service.ai.queue.step.dto.StepType;
import by.gdev.common.model.NotificationType;
import by.gdev.common.model.UserNotification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BuildAndSendNotificationStep implements AiStep<AiNotificationPayload, Void> {

    private final MailService mailService;
    private final RetrySupport retrySupport;

    @Override
    public StepType type() {
        return StepType.SEND_NOTIFICATION;
    }

    @Override
    public StepResult<Void> execute(AiNotificationPayload payload) {
        return retrySupport.retry(3, 1500, () -> {
            try {
                var user = payload.getUser();

                UserNotification n = new UserNotification();
                n.setType(NotificationType.AUTO_REPLY);

                if (user.isDefaultSendType()) {
                    String html = buildAiReplyEmailTemplate(payload);
                    n.setMessage(html);
                    n.setToMail(user.getEmail());
                    mailService.sendMessage(n).subscribe();
                } else {
                    n.setMessage(payload.getDecision().reply());
                    n.setToMail(user.getTelegram().toString());
                    mailService.sendMessageToTelegram(n);
                }

                return StepResult.ok(null);

            } catch (Exception e) {
                return StepResult.fail();
            }
        });
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

