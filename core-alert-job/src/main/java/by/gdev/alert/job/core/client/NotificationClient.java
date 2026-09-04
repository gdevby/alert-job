package by.gdev.alert.job.core.client;

import by.gdev.alert.job.core.model.credential.dto.CredentialValidationRequest;
import by.gdev.alert.job.core.model.credential.dto.CredentialValidationResult;
import by.gdev.common.model.HeaderName;
import by.gdev.common.model.SiteName;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationClient {
    private static final String VALIDATION_TIMEOUT_MESSAGE =
            "Проверка аккаунта заняла слишком много времени. Дождитесь OTP-письма и попробуйте снова.";

    private final RestTemplate restTemplate;

    @Qualifier("plainRestTemplate")
    private final RestTemplate plainRestTemplate;


    @Value("${notication.module.url}")
    private String noticationServiceUrl;

    public boolean canParse(String siteName) {
        String url = noticationServiceUrl + "/api/v1/parsers/can-parse?site=" + siteName;
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
            String url = noticationServiceUrl + "/api/v1/parsers/supported-sites";
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

    /**
     * Вызывает перераспределение прокси в notification-сервисе.
     */
    public void reassignProxies() {
        try {
            String url = noticationServiceUrl + "/api/internal/reassign-proxies";
            ResponseEntity<Void> response = restTemplate.postForEntity(url, null, Void.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.debug("Успешно вызвано перераспределение прокси в Notification");
            } else {
                log.warn("Не удалось вызвать перераспределение прокси, статус: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Ошибка при вызове перераспределения прокси в Notification", e);
        }
    }

    /**
     * Вызывает проверку аккаунта в notification-сервисе.
     * Возвращает CredentialValidationResult с информацией об ошибке, если валидация не удалась.
     */
    public CredentialValidationResult validateCredentials(String uuid, Long siteId, String login, String password) {
        try {
            CredentialValidationRequest request = new CredentialValidationRequest();
            request.setSiteId(siteId);
            request.setLogin(login);
            request.setPassword(password);

            HttpHeaders headers = new HttpHeaders();
            headers.set(HeaderName.UUID_USER_HEADER, uuid);
            headers.set("Content-Type", "application/json");

            HttpEntity<CredentialValidationRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Object> response = plainRestTemplate.exchange(
                    noticationServiceUrl + "/notification/api/ai/credentials/validate",
                    HttpMethod.POST,
                    entity,
                    Object.class
            );

            ObjectMapper mapper = new ObjectMapper();
            return mapper.convertValue(response.getBody(), CredentialValidationResult.class);

        } catch (HttpClientErrorException e) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                return mapper.readValue(e.getResponseBodyAsString(), CredentialValidationResult.class);
            } catch (Exception ex) {
                log.error("Не удалось распарсить тело ошибки: {}", e.getResponseBodyAsString());
                return CredentialValidationResult.fail("Ошибка: " + e.getStatusCode());
            }

        } catch (HttpServerErrorException e) {
            log.error("Ошибка валидации (HTTP {}): {}", e.getStatusCode().value(), e.getMessage());
            if (e.getStatusCode().value() == 504) {
                return CredentialValidationResult.fail(VALIDATION_TIMEOUT_MESSAGE);
            }
            return CredentialValidationResult.fail("Ошибка проверки аккаунта: " + e.getStatusCode());
        } catch (ResourceAccessException e) {
            log.error("Таймаут при валидации учётных данных", e);
            return CredentialValidationResult.fail(VALIDATION_TIMEOUT_MESSAGE);
        } catch (Exception e) {
            log.error("Ошибка валидации", e);
            return CredentialValidationResult.fail("Ошибка: " + e.getMessage());
        }
    }





}