package by.gdev.alert.job.core.configuration.category;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "alert.admin")
@Data
public class AdminProperties {
    private List<String> uuids;
}

