package by.gdev.alert.job.core.client;

import by.gdev.alert.job.core.model.ai.AiOrderRequest;
import by.gdev.alert.job.core.model.promt.dto.PromtResponse;
import by.gdev.alert.job.core.model.template.dto.TemplateResponse;
import by.gdev.common.model.HeaderName;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
@Slf4j
public class LlmClient {
    private final RestTemplate restTemplate;

    @Value("${llm.module.url}")
    private String llmModuleUrl;

    @Value("${llm.api.orders}")
    private String ordersApi;

    @Value("${llm.api.orders.context}")
    private String ordersContextApi;

    @Value("${llm.api.template.exists}")
    private String templateExistsApi;

    @Value("${llm.api.promts.exists}")
    private String promtExistsApi;

    public LlmClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // -----------------------------
    // Отправка AiOrderRequest
    // -----------------------------
    public void sendAiOrderRequest(AiOrderRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<AiOrderRequest> entity = new HttpEntity<>(request, headers);

            String url = llmModuleUrl + ordersContextApi;

            log.debug("Отправляю {} заказов в LLM для пользователя {} / модуля {}",
                    request.getOrders().size(),
                    request.getUser().getEmail(),
                    request.getModule().getName()
            );

            restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);
            log.debug("LLM успешно принял заказы");

        } catch (Exception e) {
            log.error("Ошибка при отправке AiOrderRequest в LLM: {}", e.getMessage(), e);
        }
    }

    // -----------------------------
    // Проверка существования шаблона
    // -----------------------------
    public boolean templateExists(Long templateId, String uuid) {
        try {
            String url = llmModuleUrl + templateExistsApi.replace("{id}", templateId.toString());
            HttpHeaders headers = new HttpHeaders();
            headers.set(HeaderName.UUID_USER_HEADER, uuid);
            HttpEntity<?> entity = new HttpEntity<>(headers);
            ResponseEntity<Boolean> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, Boolean.class);

            return Boolean.TRUE.equals(response.getBody());

        } catch (Exception e) {
            log.warn("Шаблон {} не существует или LLM недоступен", templateId);
            return false;
        }
    }

    // -----------------------------
    // Проверка существования промта
    // -----------------------------
    public boolean promtExists(Long promtId, String uuid) {
        try {
            String url = llmModuleUrl + promtExistsApi.replace("{id}", promtId.toString());
            HttpHeaders headers = new HttpHeaders();
            headers.set(HeaderName.UUID_USER_HEADER, uuid);
            HttpEntity<?> entity = new HttpEntity<>(headers);
            ResponseEntity<Boolean> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, Boolean.class);
            return Boolean.TRUE.equals(response.getBody());
        } catch (Exception e) {
            log.warn("Промт {} не существует или LLM недоступен", promtId);
            return false;
        }
    }


    public TemplateResponse getTemplate(Long templateId, String uuid) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HeaderName.UUID_USER_HEADER, uuid);
        HttpEntity<?> entity = new HttpEntity<>(headers);
        String url = llmModuleUrl + "/api/templates/" + templateId;
        return restTemplate.exchange(url, HttpMethod.GET, entity, TemplateResponse.class).getBody();
    }

    public PromtResponse getPromt(Long promtId, String uuid) {
        try {
            String url = llmModuleUrl + "/api/prompts/" + promtId;
            HttpHeaders headers = new HttpHeaders();
            headers.set(HeaderName.UUID_USER_HEADER, uuid);
            HttpEntity<?> entity = new HttpEntity<>(headers);
            return restTemplate.exchange(url, HttpMethod.GET, entity, PromtResponse.class).getBody();
        } catch (Exception e) {
            log.error("Ошибка получения промта {}", promtId, e);
            return null;
        }
    }

}
