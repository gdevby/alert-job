package by.gdev.test.service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import lombok.Data;

@RefreshScope
@ConfigurationProperties(prefix = "test.service")
@Data
public class ApplicationProperty {

	private String test;

}
