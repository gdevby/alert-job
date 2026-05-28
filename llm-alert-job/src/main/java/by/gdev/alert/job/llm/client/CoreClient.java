package by.gdev.alert.job.llm.client;

import by.gdev.alert.job.llm.domain.dto.order.AiAppUserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class CoreClient {

    private final RestTemplate restTemplate;

    @Value("${core.module.url}")
    private String coreUrl;

    public AiAppUserDTO getUserByUuid(String uuid) {
        String url = coreUrl + "/api/users/" + uuid;
        try {
            return restTemplate.getForObject(url, AiAppUserDTO.class);
        } catch (HttpClientErrorException.NotFound e) {
            return null; // пользователь не найден
        }
    }
}
