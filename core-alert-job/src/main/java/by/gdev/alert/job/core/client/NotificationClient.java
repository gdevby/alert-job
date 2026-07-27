package by.gdev.alert.job.core.client;

import by.gdev.common.model.SiteName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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

    /**
     * Получает список поддерживаемых сайтов из парсер-модуля.
     * Возвращает список SiteName.
     */
    public List<SiteName> getSupportedSites() {
        try {
            String url = parserServiceUrl + "/api/v1/parsers/supported-sites";
            String[] siteNames = restTemplate.getForObject(url, String[].class);
            if (siteNames == null) {
                return Collections.emptyList();
            }
            return Arrays.stream(siteNames)
                    .map(name -> {
                        try {
                            return SiteName.valueOf(name);
                        } catch (IllegalArgumentException e) {
                            log.warn("Неизвестное имя сайта от парсера: {}", name);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Ошибка получения списка сайтов из парсера", e);
            return Collections.emptyList();
        }
    }


    public static class ParserSupportResponse {
        public String site;
        public boolean supported;
    }
}
