package by.gdev.alert.job.notification.service.ai.otp.email;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mailbox")
@Data
public class GdevEmailConfig {
    private String host;
    private String username;
    private String password;
    private String folder;
}
