package by.gdev.alert.job.llm.client;

import by.gdev.alert.job.llm.domain.dto.order.AiAppUserDTO;
import by.gdev.alert.job.llm.domain.dto.order.AiOrderModulesDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;

/**
 * Клиент для взаимодействия с CORE‑модулем.
 * <p>
 * Используется для получения информации о пользователях по их UUID.
 * Выполняет HTTP‑запросы к CORE‑сервису через {@link RestTemplate}.
 * <p>
 * Основные особенности:
 * <ul>
 *     <li>Инкапсулирует логику вызовов CORE API.</li>
 *     <li>Возвращает {@code null}, если пользователь не найден (HTTP 404).</li>
 *     <li>Использует базовый URL из конфигурации приложения.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class CoreClient {

    /**
     * HTTP‑клиент для выполнения запросов к CORE‑сервису.
     */
    private final RestTemplate restTemplate;

    /**
     * Базовый URL CORE‑модуля, задаётся через конфигурацию.
     * Например: {@code http://core-service:8080}
     */
    @Value("${core.module.url}")
    private String coreUrl;

    /**
     * Получает информацию о пользователе по его UUID.
     *
     * @param uuid уникальный идентификатор пользователя
     * @return DTO пользователя или {@code null}, если пользователь не найден
     */
    public AiAppUserDTO getUserByUuid(String uuid) {
        String url = coreUrl + "/api/users/" + uuid;
        try {
            return restTemplate.getForObject(url, AiAppUserDTO.class);
        } catch (HttpClientErrorException.NotFound e) {
            return null; // пользователь не найден
        }
    }

    public List<AiOrderModulesDTO> getUserModules(String uuid) {
        String url = coreUrl + "/api/user/order-module";

        HttpHeaders headers = new HttpHeaders();
        headers.set("UUID-user-header", uuid);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<AiOrderModulesDTO[]> response =
                    restTemplate.exchange(url, HttpMethod.GET, entity, AiOrderModulesDTO[].class);
            AiOrderModulesDTO[] modules = response.getBody();
            if (modules == null) {
                return List.of();
            }
            return Arrays.stream(modules)
                    .map(m -> {
                        AiOrderModulesDTO dto = new AiOrderModulesDTO();
                        dto.setId(m.getId());
                        dto.setName(m.getName());
                        return dto;
                    })
                    .toList();

        } catch (Exception e) {
            return List.of();
        }
    }

    public String getModuleName(String uuid, Long moduleId) {
        return getUserModules(uuid).stream()
                .filter(m -> m.getId().equals(moduleId))
                .map(AiOrderModulesDTO::getName)
                .findFirst()
                .orElse("UNKNOWN");
    }
}
