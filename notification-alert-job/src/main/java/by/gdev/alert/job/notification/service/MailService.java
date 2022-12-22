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
import by.gdev.alert.job.notification.model.UserMail;
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

	public Mono<Void> sendMessage(UserMail userMail) {
		return Mono.defer(() -> {
			Email mail = EmailBuilder.startingBlank().from(property.getMailLogin()).to(userMail.getToMail())
					.withSubject("Email").withHTMLText(userMail.getMessage()).buildEmail();
			mailer.sendMail(mail);
			return Mono.empty();
		});
	}

	public Mono<Void> sendMessageToTelegram(UserMail userMail) {
		return Mono.defer(() -> {
			MessageData m = new MessageData(Integer.valueOf(userMail.getToMail()), userMail.getMessage());
			webClient.post()
					.uri("https://api.telegram.org",
							u -> u.path("/bot{token}/sendMessage").build(property.getTelegramChatToken()))
					.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).bodyValue(m).retrieve()
					.toBodilessEntity().doOnSuccess(r -> log.info("mesaged user {}, {}", userMail.getToMail(), userMail.getMessage())).subscribe();
			return Mono.empty();
		});
	}

}