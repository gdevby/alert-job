package by.gdev.alert.job.notification.service.ai.queue.step.impl;

import by.gdev.alert.job.notification.model.dto.AiNotificationPayload;
import by.gdev.alert.job.notification.service.MailService;
import by.gdev.alert.job.notification.service.ai.queue.step.AiStep;
import by.gdev.alert.job.notification.service.ai.queue.step.RetrySupport;
import by.gdev.alert.job.notification.service.ai.queue.step.dto.StepResult;
import by.gdev.alert.job.notification.service.ai.queue.step.dto.StepType;
import by.gdev.common.model.NotificationType;
import by.gdev.common.model.NotificationTypeEnum;
import by.gdev.common.model.UserNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
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
        log.info("АВТООТВЕТ: {} -> НАЧАЛО ОТПРАВКИ УВЕДОМЛЕНИЯ, пользователь: {}, тип уведомления: {}",
                payload.getModule().getName(), payload.getUser().getEmail(), payload.getNotificationType());
        return retrySupport.retry(3, 1500, () -> {
            try {
                var user = payload.getUser();
                NotificationTypeEnum type = payload.getNotificationType();

                if (type == null || type == NotificationTypeEnum.NONE) {
                    log.info("АВТООТВЕТ: {} -> тип уведомления NONE, пропускаем отправку", payload.getModule().getName());
                    return StepResult.ok(null);
                }

                UserNotification n = new UserNotification();
                n.setType(NotificationType.AUTO_REPLY);

                if (NotificationTypeEnum.EMAIL.equals(type)) {
                    log.info("АВТООТВЕТ: {} -> подготовка EMAIL для: {}", payload.getModule().getName(), user.getEmail());
                    String html = buildAiReplyEmailTemplate(payload);
                    String plainText = payload.getDecision().reply();
                    log.info("АВТООТВЕТ: {} -> EMAIL ТЕКСТ: {}", payload.getModule().getName(), plainText);
                    log.info("АВТООТВЕТ: {} -> EMAIL HTML: {}", payload.getModule().getName(), html);
                    n.setMessage(html);
                    n.setToMail(user.getEmail());

                    String attachmentContent = buildAttachmentContent(payload);
                    log.info("АВТООТВЕТ: {} -> отправка email с вложением, размер вложения: {} байт",
                            payload.getModule().getName(), attachmentContent.getBytes(StandardCharsets.UTF_8).length);
                    mailService.sendMessageWithAttachment(n, "response_ai.txt", attachmentContent.getBytes(StandardCharsets.UTF_8))
                            .subscribe();

                    log.info("АВТООТВЕТ: {} -> EMAIL отправлен на {}", payload.getModule().getName(), user.getEmail());

                } else {
                    String telegramText = payload.getDecision().reply();
                    log.info("АВТООТВЕТ: {} -> TELEGRAM ТЕКСТ: {}", payload.getModule().getName(), telegramText);
                    log.info("АВТООТВЕТ: {} -> подготовка TELEGRAM для: {}", payload.getModule().getName(), user.getTelegram());
                    n.setMessage(telegramText);
                    n.setToMail(user.getTelegram().toString());
                    mailService.sendMessageToTelegram(n);
                    log.info("АВТООТВЕТ: {} -> TELEGRAM отправлен на {}", payload.getModule().getName(), user.getTelegram());
                }
                log.info("АВТООТВЕТ: {} -> УВЕДОМЛЕНИЕ УСПЕШНО ОТПРАВЛЕНО", payload.getModule().getName());
                return StepResult.ok(null);

            } catch (Exception e) {
                log.warn("АВТООТВЕТ: {} -> ОШИБКА ОТПРАВКИ УВЕДОМЛЕНИЯ: {}", payload.getModule().getName(), e.getMessage(), e);
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
                            
                            <p style="margin-top: 12px; color: #666; font-size: 12px;">
                                📎 Полный ответ приложен к письму как файл response_ai.txt
                            </p>
                        </div>
                        """,
                payload.getModule().getName(),
                payload.getOrder().getTitle(),
                payload.getOrder().getLink(),
                payload.getOrder().getLink(),
                replyHtml
        );
    }

    /**
     * Формирует содержимое для вложения с полным ответом AI.
     */
    private String buildAttachmentContent(AiNotificationPayload payload) {
        return String.format("""
                ======================================
                ОТВЕТ AI НА ЗАКАЗ
                ======================================

                Модуль: %s
                Название заказа: %s
                Ссылка: %s
                Дата: %s

                ======================================
                ТЕКСТ ОТВЕТА
                ======================================

                %s

                ======================================
                КОНЕЦ СООБЩЕНИЯ
                ======================================
                """,
                payload.getModule().getName(),
                payload.getOrder().getTitle(),
                payload.getOrder().getLink(),
                java.time.LocalDateTime.now(),
                payload.getDecision().reply()
        );
    }
}