package by.gdev.alert.job.llm.client;

import by.gdev.alert.job.llm.domain.dto.order.AiAppUserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

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
}
