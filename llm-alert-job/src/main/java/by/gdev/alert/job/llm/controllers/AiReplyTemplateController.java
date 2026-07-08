package by.gdev.alert.job.llm.controllers;

import by.gdev.alert.job.llm.domain.AiReplyTemplate;
import by.gdev.alert.job.llm.domain.dto.template.TemplateRequest;
import by.gdev.alert.job.llm.domain.dto.template.TemplateResponse;
import by.gdev.alert.job.llm.service.template.AiReplyTemplateService;
import by.gdev.common.model.HeaderName;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Templates", description = "Управление шаблонами автоответов")
public class AiReplyTemplateController {

    private final AiReplyTemplateService templateService;

    @Operation(
            summary = "Создать или обновить шаблон",
            description = "Создаёт новый HTML‑шаблон или обновляет существующий. "
                    + "Возвращает ID созданного/обновлённого шаблона."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Шаблон успешно создан или обновлён",
            content = @Content(schema = @Schema(implementation = Long.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Ошибка валидации или бизнес‑логики"
    )
    @PostMapping
    public ResponseEntity<?> createOrUpdate(
            @Parameter(hidden = true)
            @RequestHeader(HeaderName.UUID_USER_HEADER) String uuid, @RequestBody TemplateRequest req) {
        try {
            AiReplyTemplate template = templateService.createOrUpdateTemplate(uuid, req);
            return ResponseEntity.ok(template.getId());
        } catch (Exception e) {
            log.error("Failed to create/update template", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(
            summary = "Удалить шаблон",
            description = "Удаляет шаблон по ID. Можно удалять только свои шаблоны. Системный DEFAULT_TEMPLATE удалить нельзя."
    )
    @ApiResponse(
            responseCode = "204",
            description = "Шаблон успешно удалён"
    )
    @ApiResponse(
            responseCode = "400",
            description = "Ошибка: шаблон не найден, нет прав или попытка удалить дефолтный шаблон"
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTemplate(
            @Parameter(hidden = true)
            @RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
            @PathVariable Long id) {
        try {
            templateService.deleteTemplate(uuid, id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Ошибка при удалении шаблона {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(
            summary = "Получить шаблоны пользователя",
            description = "Возвращает список всех шаблонов, созданных пользователем."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Список шаблонов",
            content = @Content(schema = @Schema(implementation = TemplateResponse.class))
    )
    @GetMapping("/user/my")
    public ResponseEntity<?> getTemplatesByUser(
            @Parameter(hidden = true)
            @RequestHeader(HeaderName.UUID_USER_HEADER) String uuid) {
        try {
            List<AiReplyTemplate> templates = templateService.getTemplatesByUser(uuid);

            List<TemplateResponse> result = templates.stream().map(t -> {
                TemplateResponse dto = new TemplateResponse();
                dto.setName(t.getName());
                dto.setId(t.getId());
                dto.setText(t.getText());
                dto.setCreatedAt(
                        t.getCreatedAt() != null
                                ? t.getCreatedAt().toString()
                                : LocalDateTime.now().toString()
                );
                return dto;
            }).toList();

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Failed to load templates for user {}", uuid, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    @Operation(
            summary = "Проверить существование шаблона",
            description = "Возвращает true/false в зависимости от того, существует ли шаблон."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Результат проверки"
    )
    @GetMapping("/{id}/exists")
    public ResponseEntity<?> exists(
            @Parameter(hidden = true)
            @RequestHeader(HeaderName.UUID_USER_HEADER) String uuid, @PathVariable Long id) {
        try {
            boolean exists = templateService.exists(uuid, id);
            return ResponseEntity.ok(exists);
        } catch (Exception e) {
            log.error("Failed to check template {}", id, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(
            summary = "Получить шаблон по ID",
            description = "Возвращает полную информацию о шаблоне."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Шаблон найден",
            content = @Content(schema = @Schema(implementation = TemplateResponse.class))
    )
    @GetMapping("/{id}")
    public ResponseEntity<?> getTemplateById(
            @Parameter(hidden = true)
            @RequestHeader(HeaderName.UUID_USER_HEADER) String uuid, @PathVariable Long id) {
        try {
            AiReplyTemplate t = templateService.getTemplateById(uuid, id);
            TemplateResponse dto = new TemplateResponse();
            dto.setId(t.getId());
            dto.setName(t.getName());
            dto.setText(t.getText());
            dto.setCreatedAt(
                    t.getCreatedAt() != null
                            ? t.getCreatedAt().toString()
                            : LocalDateTime.now().toString()
            );
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}
