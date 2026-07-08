package by.gdev.alert.job.llm.controllers.promt;

import by.gdev.alert.job.llm.domain.dto.promt.PromptRequest;
import by.gdev.alert.job.llm.domain.promt.AiPrompt;
import by.gdev.alert.job.llm.domain.dto.promt.AiPromptDto;
import by.gdev.alert.job.llm.service.aiautoreply.promt.AiPromptService;
import by.gdev.common.model.HeaderName;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prompts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Prompts", description = "Управление AI-промтами")
public class AiPromptController {

    private final AiPromptService promptService;

    @Operation(
            summary = "Создать или обновить промт пользователя",
            description = "Создаёт новый промт или обновляет существующий по имени. Версия увеличивается автоматически."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Промт создан или обновлён",
            content = @Content(schema = @Schema(implementation = Long.class))
    )
    @PostMapping("/create")
    public ResponseEntity<?> createOrUpdate(
            @RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
            @RequestBody PromptRequest request
    ) {
        try {
            AiPromptDto  prompt = promptService.createOrUpdatePrompt(uuid, request.getName(),
                    request.getText());
            return ResponseEntity.ok(prompt.getId());
        } catch (Exception e) {
            log.error("Failed to create/update prompt", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(
            summary = "Получить все промты пользователя",
            description = "Возвращает список всех промтов, созданных пользователем."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Список промтов",
            content = @Content(schema = @Schema(implementation = AiPromptDto.class))
    )
    @GetMapping("/user/my")
    public ResponseEntity<?> getPromptsByUser(
            @RequestHeader(HeaderName.UUID_USER_HEADER) String uuid) {
        try {
            List<AiPromptDto> dtos = promptService.getAllPromptDtos(uuid);
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            log.error("Failed to load prompts for user {}", uuid, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(
            summary = "Получить промт по ID",
            description = "Возвращает полную информацию о промте."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Промт найден",
            content = @Content(schema = @Schema(implementation = AiPrompt.class))
    )
    @GetMapping("/{id}")
    public ResponseEntity<?> getPromptById(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid, @PathVariable Long id) {
        try {
            AiPromptDto prompt = promptService.getPromptByIdOrDefault(uuid, id);
            return ResponseEntity.ok(prompt);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(
            summary = "Получить промт по умолчанию",
            description = "Возвращает глобальный DEFAULT_PROMPT."
    )
    @ApiResponse(
            responseCode = "200",
            description = "DEFAULT_PROMPT"
    )
    @GetMapping("/default")
    public ResponseEntity<?> getDefaultPrompt() {
        try {
            AiPrompt prompt = promptService.getDefaultPromptEntity();
            return ResponseEntity.ok(prompt);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(
            summary = "Удалить промт",
            description = "Удаляет промт по ID. Можно удалять только свои промты. Системный DEFAULT_PROMPT удалить нельзя."
    )
    @ApiResponse(
            responseCode = "204",
            description = "Промт успешно удалён"
    )
    @ApiResponse(
            responseCode = "400",
            description = "Ошибка: промт не найден, нет прав или попытка удалить дефолтный промт"
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePrompt(
            @RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
            @PathVariable Long id) {
        try {
            promptService.deletePrompt(uuid, id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Ошибка при удалении промта {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(
            summary = "Проверить существование промта",
            description = "Возвращает true, если промт существует и доступен пользователю (глобальный или принадлежит пользователю)."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Результат проверки",
            content = @Content(schema = @Schema(implementation = Boolean.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Ошибка валидации"
    )
    @GetMapping("/{id}/exists")
    public ResponseEntity<Boolean> checkPromptExists(@RequestHeader(HeaderName.UUID_USER_HEADER) String uuid, @PathVariable Long id) {
        boolean exists = promptService.existsById(uuid, id);
        return ResponseEntity.ok(exists);
    }

}
