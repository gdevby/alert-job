package by.gdev.alert.job.email.config;

import org.springframework.beans.factory.annotation.Value;

import lombok.Data;

@Data
public class ApplicationProperty {

	@Value("${mail.login}")
	private String mailLogin;
	
	@Value("${mail.password}")
	private String mailPassword;
	
	
}
