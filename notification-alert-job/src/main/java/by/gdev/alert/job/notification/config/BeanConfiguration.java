package by.gdev.alert.job.notification.config;

import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.api.mailer.config.TransportStrategy;
import org.simplejavamail.mailer.MailerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {

	@Bean
	public ApplicationProperty property() {
		return new ApplicationProperty();
	}

	@Bean
	public Mailer test() {
		return MailerBuilder.withSMTPServerHost("smtp.gmail.com").withSMTPServerPort(587)
				.withSMTPServerUsername(property().getMailLogin()).withSMTPServerPassword(property().getMailPassword())
				.withTransportStrategy(TransportStrategy.SMTP_TLS).withDebugLogging(false).buildMailer();

	}

}
