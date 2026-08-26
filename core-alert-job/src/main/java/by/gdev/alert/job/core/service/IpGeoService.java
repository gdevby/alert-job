package by.gdev.alert.job.core.service;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class IpGeoService {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String API_URL = "http://ip-api.com/json/";

    public String getCountryByIp(String ip) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + ip + "?fields=status,country"))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            Map<String, Object> map = objectMapper.readValue(response.body(), Map.class);

            if ("success".equals(map.get("status"))) {
                return (String) map.get("country");
            }
        } catch (Exception e) {
            log.warn("Failed to get country for IP {}: {}", ip, e.getMessage());
        }
        return null;
    }
}