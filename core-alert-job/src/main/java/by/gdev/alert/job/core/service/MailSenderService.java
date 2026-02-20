package by.gdev.alert.job.core.service;

import by.gdev.alert.job.core.model.db.AppUser;
import by.gdev.alert.job.core.repository.AppUserRepository;
import by.gdev.common.model.UserNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
public class MailSenderService {

    private final AppUserRepository userRepository;
    private final WebClient webClient;

    @Value("${telegram.max.failures:5}")
    private int maxTelegramFailures;

    private static final String NEW_LINE = "\n";
    private static final String SEND_MESSAGE_URL_TELEGRAM = "http://notification:8019/telegram";
    private static final String SEND_MESSAGE_URL_MAIL = "http://notification:8019/mail";
    private static final String TELEGRAM_WARNING_MESSAGE = "Здравствуйте! Обнаружены проблемы с отправкой уведомлений в Telegram. "
            + "Проверьте, не заблокирован ли бот. Временные уведомления будут приходить на email.";


    public void sendTelegramIssueNotification(AppUser user) {
        UserNotification notification = new UserNotification(user.getEmail(), TELEGRAM_WARNING_MESSAGE);

        webClient.post()
                .uri(SEND_MESSAGE_URL_MAIL)
                .bodyValue(notification)
                .retrieve()
                .bodyToMono(Void.class)
                .subscribe(
                        success -> log.info("Sent Telegram issue notification to user {}", user.getUuid()),
                        error -> log.error("Failed to send Telegram issue notification to user {}", user.getUuid(), error)
                );
    }

    public void sendMessagesToUser(AppUser user, List<String> messages) {
        if(!user.isSwitchOffAlerts()){
            return;
        }

        // Нужно ли использовать email из-за ошибок Telegram
        boolean useEmail = false;

        if (!user.isDefaultSendType()) {
            Integer failCount = user.getTelegramFailCount();
            if (failCount != null && failCount >= maxTelegramFailures) {
                log.info("User {} has {} Telegram failures, using email",
                        user.getUuid(), failCount);
                useEmail = true;

                if (user.isSwitchOffAlerts()){ //если у пользователя были включены уведомления
                    sendTelegramIssueNotification(user); //Отправляем сообщение о проблемах
                    user.setSwitchOffAlerts(false); // выключаем уведомления у него
                    userRepository.save(user); //сохраняем новые параметры пользователя
                    return; //выходим
                }
            }
        }

        // Выбираем способ отправки
        String uri = user.isDefaultSendType() || useEmail ? SEND_MESSAGE_URL_MAIL : SEND_MESSAGE_URL_TELEGRAM;

        // Отправляем сообщения
        boolean success = sendMessageBatch(user, uri, messages);

        // Обрабатываем результат
        if (!success && uri.equals(SEND_MESSAGE_URL_TELEGRAM)) {
            // Ошибка Telegram
            int newCount = user.getTelegramFailCount() == null ? 1 : user.getTelegramFailCount() + 1;
            user.setTelegramFailCount(newCount);
            userRepository.save(user);

            log.info("Telegram send failed for user {}, fail count: {}",
                    user.getUuid(), newCount);

            if (newCount >= maxTelegramFailures) {
                log.warn("User {} reached {} Telegram failures",
                        user.getUuid(), maxTelegramFailures);
            }
        }
        else if (success && uri.equals(SEND_MESSAGE_URL_TELEGRAM)) {
            // Успех Telegram - сбрасываем счетчик
            if (user.getTelegramFailCount() != null && user.getTelegramFailCount() > 0) {
                user.setTelegramFailCount(0);
                userRepository.save(user);
                log.info("Telegram success for user {}, reset fail count", user.getUuid());
            }
        }
    }

    private boolean sendMessageBatch(AppUser user, String uri, List<String> messages) {
        try {
            StringBuilder sb = new StringBuilder();
            for (String msg : messages) {
                sb.append(msg).append(NEW_LINE);
                if (sb.length() > 3000) {
                    if (!sendSingleMessage(user, uri, sb.substring(0, sb.length() - 1))) {
                        return false;
                    }
                    sb.setLength(0);
                }
            }
            if (sb.length() > 0) {
                return sendSingleMessage(user, uri, sb.substring(0, sb.length() - 1));
            }
            return true;
        } catch (Exception e) {
            log.debug("Error sending message batch to user {}: {}", user.getUuid(), e.getMessage());
            return false;
        }
    }

    private boolean sendSingleMessage(AppUser user, String uri, String message) {
        UserNotification un;

        if (uri.equals(SEND_MESSAGE_URL_MAIL)) {
            un = new UserNotification(user.getEmail(), message);
        } else {
            un = new UserNotification(String.valueOf(user.getTelegram()), message);
        }

        try {
            webClient.post()
                    .uri(uri)
                    .bodyValue(un)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .subscribe(
                            success -> log.debug("Message sent successfully to user {} via {}",
                                    user.getUuid(), uri.contains("telegram") ? "Telegram" : "Email"),
                            error -> log.debug("Failed to send message to user {} via {}: {}",
                                    user.getUuid(), uri.contains("telegram") ? "Telegram" : "Email", error.getMessage())
                    );

            return true;
        } catch (Exception ex) {
            log.debug("Failed to send message to user {} via {}: {}",
                    user.getUuid(), uri.contains("telegram") ? "Telegram" : "Email", ex.getMessage());
            return false;
        }
    }

}
