package by.gdev.alert.job.notification.service.ai.credential;

import lombok.Getter;
import org.springframework.web.reactive.function.client.WebClient;

@Getter
public class CoreWebClient {

    private final WebClient client;

    public CoreWebClient(WebClient client) {
        this.client = client;
    }
}
