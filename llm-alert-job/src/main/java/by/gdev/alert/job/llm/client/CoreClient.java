package by.gdev.alert.job.llm.client;

import by.gdev.alert.job.llm.domain.dto.order.AiAppUserDTO;
import by.gdev.alert.job.llm.domain.dto.order.AiOrderModulesDTO;
import by.gdev.common.model.HeaderName;
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

    public List<AiOrderModulesDTO> getUserModules(String uuid) {
        String url = coreUrl + "/api/user/order-module";

        HttpHeaders headers = new HttpHeaders();
        headers.set(HeaderName.UUID_USER_HEADER, uuid);
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
