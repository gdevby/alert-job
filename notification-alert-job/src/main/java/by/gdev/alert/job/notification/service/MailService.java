package by.gdev.alert.job.notification.service;

import org.simplejavamail.api.email.Email;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.email.EmailBuilder;
import org.springframework.stereotype.Service;

import by.gdev.alert.job.notification.config.ApplicationProperty;
import by.gdev.alert.job.notification.model.UserMail;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Data
@Service
@RequiredArgsConstructor
public class MailService {

	private final ApplicationProperty property;
	private final Mailer mailer;

	public Mono<Void> sendMessage(UserMail userMail) {
		return  Mono.defer(()-> {
			Email mail = EmailBuilder.startingBlank().from(property.getMailLogin()).to(userMail.getToMail())
					.withSubject("Email").withHTMLText(userMail.getMessage()).buildEmail();
			mailer.sendMail(mail);
			return Mono.empty();
		});
	}

}