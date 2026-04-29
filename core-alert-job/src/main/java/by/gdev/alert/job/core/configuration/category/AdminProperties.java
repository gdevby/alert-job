package by.gdev.alert.job.core.configuration.category;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "alert.admin")
@Data
public class AdminProperties {
    private String uuid;
}

