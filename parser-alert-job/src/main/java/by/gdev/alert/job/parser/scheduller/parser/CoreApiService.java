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

    @Value("${orders.api.url}")
    private String ordersApiUrl;

    @Value("${core.module.url}")
    private String coreModuleUrl;

    @Value("${core.api.batch-size:100}")
    private int batchSize;

    public void sendOrders(List<OrderDTO> orders, SiteName siteName) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<List<OrderDTO>> request = new HttpEntity<>(orders, headers);
            String urlWithParam = coreModuleUrl + ordersApiUrl + "?site=" + siteName.name();
            metricsService.save(siteName, orders.size());
            statisticsService.save(siteName, orders.size());
            log.debug("Отправка заказов в core... для {}", siteName);
            restTemplate.exchange(urlWithParam, HttpMethod.POST, request, Void.class);
            log.debug("Заказы успешно отправлены в core для сайта {}", siteName);
        } catch (Exception e) {
            log.error("Ошибка при отправке заказов в core API для сайта {}: {}", siteName, e.getMessage(), e);
        }
    }

    public void sendOrdersInBatches(List<OrderDTO> orders, SiteName siteName) {
        for (int i = 0; i < orders.size(); i += batchSize) {
            List<OrderDTO> batch = orders.subList(i, Math.min(i + batchSize, orders.size()));
            sendBatch(batch, siteName);
        }
    }

    private void sendBatch(List<OrderDTO> batch, SiteName siteName) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<List<OrderDTO>> request = new HttpEntity<>(batch, headers);
            String urlWithParam = coreModuleUrl + ordersApiUrl + "?site=" + siteName.name();
            metricsService.save(siteName, batch.size());
            statisticsService.save(siteName, batch.size());
            restTemplate.exchange(urlWithParam, HttpMethod.POST, request, Void.class);
            log.debug("Отправлено {} заказов в core для {}", batch.size(), siteName);
        } catch (Exception e) {
            log.error("Ошибка при отправке пачки заказов: {}", e.getMessage(), e);
        }
    }

}
