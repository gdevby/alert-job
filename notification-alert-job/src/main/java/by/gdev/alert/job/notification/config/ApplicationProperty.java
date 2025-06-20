package by.gdev.alert.job.notification.config;

import org.simplejavamail.api.mailer.config.TransportStrategy;
import org.springframework.beans.factory.annotation.Value;

import lombok.Data;

@Data
public class ApplicationProperty {

	@Value("${mail.login}")
	private String mailLogin;
	
	@Value("${mail.password}")
	private String mailPassword;
	@Value("${telegram.chat.token}")
	private String telegramChatToken;
	
	
	@Value("${mail.smtp.username}")
	private String smtpMailUsername;	
	@Value("${mail.smtp.password}")
	private String smtpMailPassword;
	@Value("${mail.smtp.host}")
	private String smtpHost;
	@Value("${mail.smtp.port}")
	private Integer smtpPort;
	@Value("${mail.smtp.transport.strategy}")
	private TransportStrategy transportStrategy;
	
}
