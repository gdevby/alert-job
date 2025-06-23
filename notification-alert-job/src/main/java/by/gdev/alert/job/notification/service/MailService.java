package by.gdev.alert.job.notification.service;

import by.gdev.alert.job.notification.config.MetricsConfig;
import io.micrometer.core.instrument.Counter;
import org.simplejavamail.api.email.Email;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.email.EmailBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import by.gdev.alert.job.notification.config.ApplicationProperty;
import by.gdev.alert.job.notification.model.MessageData;
import by.gdev.common.model.UserNotification;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Data
@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private final ApplicationProperty property;
    private final Mailer mailer;
    private final WebClient webClient;
    private final ApplicationContext context;

    public Mono<Void> sendMessage(UserNotification userMail) {
        Counter positiveMailCounter = context.getBean(MetricsConfig.COUNTER_MAIL_POSITIVE, Counter.class);
        Counter negativeMailCounter = context.getBean(MetricsConfig.COUNTER_MAIL_NEGATIVE, Counter.class);

        return Mono.defer(() -> {
                    Email mail = EmailBuilder.startingBlank()
                            .from(property.getSmtpMailUsername())
                            .to(userMail.getToMail())
                            .withSubject("Email")
                            .withHTMLText(userMail.getMessage())
                            .buildEmail();
                    mailer.sendMail(mail);
                    return Mono.empty();
                })
                .doOnSuccess(r -> {
                    log.info("sent message for user email {}, {}", userMail.getToMail(), userMail.getMessage());
                    positiveMailCounter.increment();
                })
                .doOnError(throwable -> {
                    log.info("can't send test message for user email  {}, {}", userMail.getToMail(), userMail.getMessage());
                    negativeMailCounter.increment();
                })
                .then();
    }

    public Mono<Void> sendMessageToTelegram(UserNotification userMail) {
        Counter positiveTelegramCounter = context.getBean(MetricsConfig.COUNTER_TELEGRAM_POSITIVE, Counter.class);
        Counter negativeTelegramCounter = context.getBean(MetricsConfig.COUNTER_TELEGRAM_NEGATIVE, Counter.class);


        MessageData messageData = new MessageData(Long.valueOf(userMail.getToMail()), userMail.getMessage());
        return webClient.post()
                .uri("https://api.telegram.org",
                        uriBuilder -> uriBuilder
                                .path("/bot{token}/sendMessage")
                                .build(property.getTelegramChatToken()))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(messageData)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(r -> {
                    log.info("sent message for user telegram {}, {}", userMail.getToMail(), userMail.getMessage());
                    positiveTelegramCounter.increment();
                })
                .onErrorResume(ex -> {
                    negativeTelegramCounter.increment();
                    return Mono.error(ex);
                });
    }
}