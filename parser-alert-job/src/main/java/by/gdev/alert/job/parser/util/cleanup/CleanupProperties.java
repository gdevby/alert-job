package by.gdev.alert.job.parser.util.cleanup;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "cleanup")
public class CleanupProperties {
    private String retention;

    public String getRetention() {
        return retention;
    }
    public void setRetention(String retention) {
        this.retention = retention;
    }
}

