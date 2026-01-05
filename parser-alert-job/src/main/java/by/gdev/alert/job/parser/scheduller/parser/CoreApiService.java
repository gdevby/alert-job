package by.gdev.alert.job.parser.scheduller.parser;

import by.gdev.alert.job.parser.service.metrics.MetricsService;
import by.gdev.alert.job.parser.service.statistic.StatisticsService;
import by.gdev.alert.job.parser.util.SiteName;
import by.gdev.common.model.OrderDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoreApiService {

    private final RestTemplate restTemplate;
    private final StatisticsService statisticsService;
    private final MetricsService metricsService;

    @Value("${core.api.url}")
    private String coreApiUrl;

    public void sendOrders(List<OrderDTO> orders, SiteName siteName) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<List<OrderDTO>> request = new HttpEntity<>(orders, headers);

            String urlWithParam = coreApiUrl + "?site=" + siteName.name();
            metricsService.save(siteName, orders.size());
            statisticsService.save(siteName, orders.size());
            log.debug("Отправка заказов в core... для {}", siteName);
            restTemplate.exchange(urlWithParam, HttpMethod.POST, request, Void.class);
            log.debug("Заказы успешно отправлены в core для сайта {}", siteName);
        } catch (Exception e) {
            log.error("Ошибка при отправке заказов в core API для сайта {}: {}", siteName, e.getMessage(), e);
        }
    }
}
