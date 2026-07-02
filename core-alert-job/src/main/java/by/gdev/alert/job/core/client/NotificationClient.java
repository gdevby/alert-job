package by.gdev.alert.job.core.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationClient {
    private final RestTemplate restTemplate;

    @Value("${notication.module.url}")
    private String parserServiceUrl;

    public boolean canParse(String siteName){
        String url = parserServiceUrl + "/api/v1/parsers/can-parse?site=" + siteName;
        try {
            ResponseEntity<ParserSupportResponse> response =
                    restTemplate.getForEntity(url, ParserSupportResponse.class);
            if (response.getBody() == null) {
                log.warn("Пустой ответ от parser-service для сайта {}", siteName);
                return false;
            }
            return response.getBody().supported;
        } catch (Exception e) {
            log.error("Ошибка при запросе can-parse для {}: {}", siteName, e.getMessage());
            return false;
        }
    }

    public static class ParserSupportResponse {
        public String site;
        public boolean supported;
    }
}
