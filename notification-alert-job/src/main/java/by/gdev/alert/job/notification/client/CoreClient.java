package by.gdev.alert.job.notification.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CoreClient {

    private final RestTemplate restTemplate;

    @Value("${core.service.url}")
    private String coreUrl;

    public List<String> getUsersWithAutoReplyEnabled() {
        try {
            String url = coreUrl + "/api/modules/auto-reply/users";
            String[] users = restTemplate.getForObject(url, String[].class);
            return users != null ? Arrays.asList(users) : Collections.emptyList();
        } catch (Exception e) {
            log.error("Ошибка получения пользователей с автоответом", e);
            return Collections.emptyList();
        }
    }
}