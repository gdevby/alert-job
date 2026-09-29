package by.gdev.common.service.playwright.captcha;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * HTTP-клиент 2Captcha API v2 для Yandex SmartCaptcha (token-based).
 */
@Component
@Slf4j
public class TwoCaptchaClient {

    private static final String CREATE_TASK_URL = "https://api.2captcha.com/createTask";
    private static final String GET_RESULT_URL = "https://api.2captcha.com/getTaskResult";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${captcha.two-captcha.enabled:false}")
    private boolean enabled;

    @Value("${captcha.two-captcha.api-key:}")
    private String apiKey;

    @Value("${captcha.two-captcha.poll-interval-ms:3000}")
    private long pollIntervalMs;

    @Value("${captcha.two-captcha.timeout-ms:120000}")
    private long timeoutMs;

    @PostConstruct
    void logConfiguration() {
        if (!enabled) {
            log.info("2Captcha: выключено (captcha.two-captcha.enabled=false)");
            return;
        }
        String key = effectiveApiKey();
        if (key.isBlank()) {
            log.warn("2Captcha: включено, но captcha.two-captcha.api-key / TWO_CAPTCHA_API_KEY пустой");
            return;
        }
        log.info("2Captcha: включено, clientKey={}", maskClientKey(key));
        if (apiKey != null && !apiKey.equals(key)) {
            log.warn("2Captcha: в api-key были пробелы по краям — в запросах используется trim()");
        }
        log.debug("2Captcha: clientKey (полностью)={}", key);
    }

    public boolean isConfigured() {
        return enabled && !effectiveApiKey().isBlank();
    }

    private String effectiveApiKey() {
        return apiKey == null ? "" : apiKey.trim();
    }

    private static String maskClientKey(String key) {
        if (key == null || key.isBlank()) {
            return "<empty>";
        }
        if (key.length() <= 8) {
            return key.charAt(0) + "*** (len=" + key.length() + ")";
        }
        return key.substring(0, 4) + "..." + key.substring(key.length() - 4) + " (len=" + key.length() + ")";
    }

    /**
     * @return токен smart-captcha или empty при ошибке / отключённом сервисе
     */
    public Optional<String> solveYandexSmartCaptcha(String pageUrl, String siteKey, String userAgent, String cookies) {
        if (!isConfigured()) {
            log.warn("2Captcha: запрос пропущен — enabled={}, clientKey={}",
                    enabled, maskClientKey(effectiveApiKey()));
            return Optional.empty();
        }
        if (siteKey == null || siteKey.isBlank()) {
            log.warn("2Captcha: sitekey не найден для {}", pageUrl);
            return Optional.empty();
        }

        try {
            long taskId = createYandexTask(pageUrl, siteKey, userAgent, cookies);
            return pollTaskResult(taskId, "Yandex SmartCaptcha");
        } catch (Exception e) {
            log.error("2Captcha: не удалось решить Yandex SmartCaptcha для {}", pageUrl, e);
            return Optional.empty();
        }
    }

    /** @return hCaptcha response token (gRecaptchaResponse) */
    public Optional<String> solveHCaptcha(String pageUrl, String siteKey, String userAgent) {
        if (!isConfigured()) {
            return Optional.empty();
        }
        if (siteKey == null || siteKey.isBlank()) {
            log.warn("2Captcha: hCaptcha sitekey не найден для {}", pageUrl);
            return Optional.empty();
        }
        try {
            long taskId = createHCaptchaTask(pageUrl, siteKey, userAgent);
            return pollTaskResult(taskId, "hCaptcha");
        } catch (Exception e) {
            log.error("2Captcha: не удалось решить hCaptcha для {}", pageUrl, e);
            return Optional.empty();
        }
    }

    private long createYandexTask(String pageUrl, String siteKey, String userAgent, String cookies) throws Exception {
        Map<String, Object> task = new LinkedHashMap<>();
        task.put("type", "YandexSmartCaptchaTaskProxyless");
        task.put("websiteURL", pageUrl);
        task.put("websiteKey", siteKey);
        if (userAgent != null && !userAgent.isBlank()) {
            task.put("userAgent", userAgent);
        }
        if (cookies != null && !cookies.isBlank()) {
            task.put("cookies", cookies);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        String clientKey = effectiveApiKey();
        body.put("clientKey", clientKey);
        body.put("task", task);

        log.info("2Captcha: createTask → {} clientKey={}", CREATE_TASK_URL, maskClientKey(clientKey));
        log.debug("2Captcha: createTask clientKey (полностью)={}", clientKey);

        JsonNode response = postJson(CREATE_TASK_URL, body);
        int errorId = response.path("errorId").asInt(-1);
        if (errorId != 0) {
            throw new IllegalStateException("createTask errorId=" + errorId
                    + ", code=" + response.path("errorCode").asText()
                    + ", desc=" + response.path("errorDescription").asText());
        }
        long taskId = response.path("taskId").asLong();
        if (taskId <= 0) {
            throw new IllegalStateException("createTask: пустой taskId");
        }
        log.info("2Captcha: задача Yandex SmartCaptcha создана, taskId={}", taskId);
        return taskId;
    }

    private long createHCaptchaTask(String pageUrl, String siteKey, String userAgent) throws Exception {
        Map<String, Object> task = new LinkedHashMap<>();
        task.put("type", "HCaptchaTaskProxyless");
        task.put("websiteURL", pageUrl);
        task.put("websiteKey", siteKey);
        if (userAgent != null && !userAgent.isBlank()) {
            task.put("userAgent", userAgent);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        String clientKey = effectiveApiKey();
        body.put("clientKey", clientKey);
        body.put("task", task);

        log.info("2Captcha: createTask hCaptcha → {} clientKey={}", CREATE_TASK_URL, maskClientKey(clientKey));

        JsonNode response = postJson(CREATE_TASK_URL, body);
        int errorId = response.path("errorId").asInt(-1);
        if (errorId != 0) {
            throw new IllegalStateException("createTask errorId=" + errorId
                    + ", code=" + response.path("errorCode").asText()
                    + ", desc=" + response.path("errorDescription").asText());
        }
        long taskId = response.path("taskId").asLong();
        if (taskId <= 0) {
            throw new IllegalStateException("createTask: пустой taskId");
        }
        log.info("2Captcha: задача hCaptcha создана, taskId={}", taskId);
        return taskId;
    }

    private Optional<String> pollTaskResult(long taskId, String captchaKind) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        int pollAttempt = 0;
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(pollIntervalMs);
            pollAttempt++;

            Map<String, Object> body = Map.of(
                    "clientKey", effectiveApiKey(),
                    "taskId", taskId
            );
            JsonNode response;
            try {
                response = postJson(GET_RESULT_URL, body);
            } catch (RestClientException e) {
                log.warn("2Captcha: ошибка getTaskResult для taskId={}: {}", taskId, e.getMessage());
                continue;
            }

            int errorId = response.path("errorId").asInt(-1);
            if (errorId != 0) {
                throw new IllegalStateException("getTaskResult errorId=" + errorId
                        + ", code=" + response.path("errorCode").asText()
                        + ", desc=" + response.path("errorDescription").asText());
            }

            String status = response.path("status").asText("");
            if ("processing".equalsIgnoreCase(status)) {
                if (pollAttempt == 1 || pollAttempt % 5 == 0) {
                    long leftMs = deadline - System.currentTimeMillis();
                    log.info("2Captcha: {} taskId={} poll #{} status=processing (~{} ms left)",
                            captchaKind, taskId, pollAttempt, Math.max(0, leftMs));
                }
                continue;
            }
            if ("ready".equalsIgnoreCase(status)) {
                Optional<String> token = extractSolutionToken(response.path("solution"));
                if (token.isEmpty()) {
                    throw new IllegalStateException("getTaskResult: пустой token в solution");
                }
                log.info("2Captcha: {} решена, taskId={}, tokenLen={}", captchaKind, taskId, token.get().length());
                return token;
            }
            throw new IllegalStateException("getTaskResult: неизвестный status=" + status);
        }
        log.warn("2Captcha: timeout {} ms для taskId={}", timeoutMs, taskId);
        return Optional.empty();
    }

    private Optional<String> extractSolutionToken(JsonNode solution) {
        if (solution == null || solution.isMissingNode()) {
            return Optional.empty();
        }
        String token = solution.path("token").asText(null);
        if (token != null && !token.isBlank()) {
            return Optional.of(token);
        }
        token = solution.path("gRecaptchaResponse").asText(null);
        if (token != null && !token.isBlank()) {
            return Optional.of(token);
        }
        return Optional.empty();
    }

    private JsonNode postJson(String url, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        try {
            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            throw new RestClientException("Не удалось разобрать ответ 2Captcha: " + e.getMessage(), e);
        }
    }
}
