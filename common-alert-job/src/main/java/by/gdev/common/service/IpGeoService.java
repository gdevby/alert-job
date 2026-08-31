package by.gdev.common.service;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class IpGeoService {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    private static final String API_URL = "http://ip-api.com/json/";

    public String getCountryByIp(String ip) {
        // Если IP уже есть в кэше, возвращаем из кэша
        return cache.computeIfAbsent(ip, this::fetchCountry);
    }

    private String fetchCountry(String ip) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + ip + "?fields=status,country"))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            Map<String, Object> map = objectMapper.readValue(body, Map.class);

            if ("success".equals(map.get("status"))) {
                return (String) map.get("country");
            } else {
                log.warn("ip-api returned non-success for IP {}: {}", ip, body);
                return null;
            }
        } catch (Exception e) {
            log.warn("Failed to get country for IP {}: {}", ip, e.getMessage());
            return null;
        }
    }
}