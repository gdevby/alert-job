package by.gdev.alert.job.parser.util.cleanup;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
@ConfigurationProperties(prefix = "cleanup")
public class CleanupProperties {
    private String retention;
    private boolean runstartup = false;
}

