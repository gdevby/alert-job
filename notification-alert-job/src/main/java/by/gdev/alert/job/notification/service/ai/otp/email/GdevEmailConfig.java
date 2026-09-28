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
    /** Таймаут установки TCP/TLS-соединения IMAP, мс */
    private int connectionTimeoutMs = 15_000;
    /** Таймаут чтения IMAP, мс */
    private int readTimeoutMs = 30_000;
    /** Попыток connect при сбое (в т.ч. UnknownHostException) */
    private int connectRetries = 3;
    /** Пауза между попытками connect, мс */
    private int connectRetryDelayMs = 2_000;
}
