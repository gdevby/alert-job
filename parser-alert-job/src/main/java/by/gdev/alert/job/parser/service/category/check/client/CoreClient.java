package by.gdev.alert.job.parser.service.category.check.client;

import by.gdev.alert.job.parser.service.category.update.dto.changes.CategoryChangeDTO;
import by.gdev.alert.job.parser.service.category.update.dto.changes.CategoryChangeListDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CoreClient {

    private final RestTemplate restTemplate;

    @Value("${core.module.url}")
    private String coreUrl;

    public void sendCategoryChanges(List<CategoryChangeDTO> changes) {
        CategoryChangeListDTO dto = new CategoryChangeListDTO(changes);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<CategoryChangeListDTO> entity = new HttpEntity<>(dto, headers);

        restTemplate.postForObject(coreUrl + "/category/changes", entity, Void.class);
    }
}


