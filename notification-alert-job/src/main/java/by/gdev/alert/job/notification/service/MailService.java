package by.gdev.alert.job.notification.service;

import org.simplejavamail.api.email.Email;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.email.EmailBuilder;
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

    public Mono<Void> sendMessage(UserNotification userMail) {
	return Mono.defer(() -> {
	    Email mail = EmailBuilder.startingBlank().from(property.getMailLogin()).to(userMail.getToMail())
		    .withSubject("Email").withHTMLText(userMail.getMessage()).buildEmail();
	    mailer.sendMail(mail);
	    return Mono.empty();
	}).doOnSuccess(r -> log.info("sent message for user email {}, {}", userMail.getToMail(), userMail.getMessage()))
		.doOnError(e -> log.info("can't send test message for user email  {}, {}", userMail.getToMail(),
			userMail.getMessage()))
		.then();
    }

    public Mono<Void> sendMessageToTelegram(UserNotification userMail) {
	MessageData m = new MessageData(Long.valueOf(userMail.getToMail()), userMail.getMessage());
	return webClient.post()
		.uri("https://api.telegram.org",
			u -> u.path("/bot{token}/sendMessage").build(property.getTelegramChatToken()))
		.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).bodyValue(m).retrieve()
		.bodyToMono(Void.class).doOnSuccess(r -> log.info("sent message for user telegram {}, {}",
			userMail.getToMail(), userMail.getMessage()))
		.onErrorResume(ex -> Mono.error(ex));
    }
}