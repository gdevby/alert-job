package by.gdev.alert.job.notification.config;

import by.gdev.alert.job.notification.service.ai.credential.CoreWebClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class CoreWebClientConfig {

    @Value("${core.service.url}")
    private String coreServiceUrl;

    @Bean
    public CoreWebClient coreWebClient() {
        WebClient client = WebClient.builder()
                .baseUrl(coreServiceUrl)
                .build();

        return new CoreWebClient(client);
    }
}
