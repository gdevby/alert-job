package by.gdev.alert.job.core.controller;

import by.gdev.alert.job.core.client.NotificationClient;
import by.gdev.alert.job.core.model.SiteDTO;
import by.gdev.common.model.SiteName;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/autoreply")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Autoreply sites supporting", description = "Дополнительная информация для автоответов")
public class AutoreplySitesInfoController {
    private final NotificationClient notificationClient;

    @Operation(
            summary = "Получить список поддерживаемых сайтов",
            description = "Возвращает список всех сайтов, для которых доступен парсер автоответов, с ID и названием."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Список DTO сайтов",
            content = @Content(
                    array = @ArraySchema(schema = @Schema(implementation = SiteDTO.class))
            )
    )
    @GetMapping("/supported-sites")
    public ResponseEntity<List<SiteDTO>> getSupportedSites() {
        try {
            List<SiteName> sites = notificationClient.getSupportedSites();
            List<SiteDTO> result = sites.stream()
                    .map(s -> new SiteDTO(s.getId(), s.name()))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Ошибка получения списка поддерживаемых сайтов", e);
            return ResponseEntity.badRequest().build();
        }
    }
}