package by.gdev.alert.job.core.service.ai;

import by.gdev.alert.job.core.model.ai.AiOrderRequest;
import by.gdev.common.model.OrderDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@Slf4j
public class AiOrdersClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ai.module.url}")
    private String aiModuleUrl;

    @Value("${ai.api.orders}")
    private String aiOrdersApi;

    @Value("${ai.api.orders.context}")
    private String aiOrdersContextApi;

    public void sendOrders(List<OrderDTO> orders) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<List<OrderDTO>> request = new HttpEntity<>(orders, headers);

            String url = aiModuleUrl + aiOrdersApi;

            log.info("Отправляю {} заказов в AI...", orders.size());

            restTemplate.exchange(url, HttpMethod.POST, request, Void.class);

            log.info("Заказы успешно отправлены в AI");
        } catch (Exception e) {
            log.error("Ошибка при отправке заказов в AI: {}", e.getMessage(), e);
        }
    }

    public void sendAiOrderRequest(AiOrderRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<AiOrderRequest> entity = new HttpEntity<>(request, headers);

            String url = aiModuleUrl + aiOrdersContextApi;

            log.info("Отправляю {} заказов в AI для пользователя {} / модуля {}",
                    request.getOrders().size(),
                    request.getUser().getEmail(),
                    request.getModule().getName()
            );

            restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);

            log.info("AI успешно принял заказы");
        } catch (Exception e) {
            log.error("Ошибка при отправке AiOrderRequest в AI: {}", e.getMessage(), e);
        }
    }


}
