package by.gdev.common.service.proxy;

import by.gdev.common.model.proxy.ProxyCredentials;
import by.gdev.common.model.proxy.ProxyState;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
@Slf4j
public class ProxyAdditionalService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${proxy.ru.url}")
    private String proxyHouseUrl;

    @Value("${proxy.ru.token}")
    private String authToken;

    @Value("${proxy.ru.tariff}")
    private String tariffId;

    @Value("${proxy.ru.limit}")
    private int limit;

    public ProxyAdditionalService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public List<ProxyCredentials> fetchProxies() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Auth-Token", authToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = proxyHouseUrl + "?tariff_id=" + tariffId + "&limit=" + limit;
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseProxies(response.getBody());
            } else {
                log.warn("Failed to fetch proxies, status: {}", response.getStatusCode());
                return List.of();
            }
        } catch (Exception e) {
            log.error("Error fetching proxies from ProxyHouse", e);
            return List.of();
        }
    }

    private List<ProxyCredentials> parseProxies(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            boolean successful = root.path("successful").asBoolean();
            if (!successful) {
                log.warn("API вернул unsuccessful: {}", json);
                return List.of();
            }

            JsonNode proxiesNode = root.path("data").path("proxies");
            if (!proxiesNode.isObject()) {
                log.warn("Узел 'proxies' не является объектом: {}", proxiesNode);
                return List.of();
            }

            List<ProxyCredentials> proxies = new ArrayList<>();
            Iterator<String> fieldNames = proxiesNode.fieldNames();
            while (fieldNames.hasNext()) {
                String id = fieldNames.next();
                JsonNode proxyNode = proxiesNode.path(id);

                String ip = proxyNode.path("ip").asText();
                int httpPort = proxyNode.path("http_port").asInt();
                String login = proxyNode.path("login").asText();
                String password = proxyNode.path("password").asText();

                ProxyCredentials credentials = ProxyCredentials.builder()
                        .username(login)
                        .password(password)
                        .host(ip)
                        .port(httpPort)
                        .state(ProxyState.NEW)
                        .source(ProxySource.API)
                        .build();
                proxies.add(credentials);
            }
            log.info("Получено {} прокси из ProxyAdditionalService", proxies.size());
            return proxies;
        } catch (Exception e) {
            log.error("Ошибка парсинга ответа ProxyAdditionalService", e);
            return List.of();
        }
    }
}