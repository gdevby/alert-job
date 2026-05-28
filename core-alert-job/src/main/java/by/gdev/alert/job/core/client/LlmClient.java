package by.gdev.alert.job.core.client;

import by.gdev.alert.job.core.model.ai.AiOrderRequest;
import by.gdev.common.model.OrderDTO;
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

    public LlmClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // -----------------------------
    // Отправка заказов
    // -----------------------------
    public void sendOrders(List<OrderDTO> orders) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<List<OrderDTO>> request = new HttpEntity<>(orders, headers);

            String url = llmModuleUrl + ordersApi;

            log.debug("Отправляю {} заказов в LLM...", orders.size());
            restTemplate.exchange(url, HttpMethod.POST, request, Void.class);
            log.debug("Заказы успешно отправлены в LLM");

        } catch (Exception e) {
            log.error("Ошибка при отправке заказов в LLM: {}", e.getMessage(), e);
        }
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
    public boolean templateExists(Long templateId) {
        try {
            String url = llmModuleUrl + templateExistsApi.replace("{id}", templateId.toString());

            ResponseEntity<Boolean> response =
                    restTemplate.getForEntity(url, Boolean.class);

            return Boolean.TRUE.equals(response.getBody());

        } catch (Exception e) {
            log.warn("Шаблон {} не существует или LLM недоступен", templateId);
            return false;
        }
    }
}
