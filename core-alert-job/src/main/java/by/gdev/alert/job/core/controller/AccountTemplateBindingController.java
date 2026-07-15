package by.gdev.alert.job.core.controller;

import by.gdev.alert.job.core.model.binding.dto.BindingCreateRequest;
import by.gdev.alert.job.core.model.binding.dto.BindingResponse;
import by.gdev.alert.job.core.model.binding.dto.BindingUpdateRequest;
import by.gdev.alert.job.core.model.db.ai.AccountTemplateBinding;
import by.gdev.alert.job.core.service.ai.AccountTemplateBindingService;
import by.gdev.common.model.HeaderName;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
@RequestMapping("/api/bindings")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Account Template Bindings", description = "Управление привязкой шаблонов к аккаунтам и модулям")
public class AccountTemplateBindingController {

    private final AccountTemplateBindingService service;

    @Operation(
            summary = "Создать новую привязку",
            description = "Создаёт связь между модулем, аккаунтом и шаблоном с указанием активности."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Привязка успешно создана",
            content = @Content(schema = @Schema(implementation = AccountTemplateBinding.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Ошибка валидации или бизнес-логики"
    )
    @PostMapping
    public ResponseEntity<BindingResponse> create(
            @Parameter(hidden = true)
            @RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
            @RequestBody BindingCreateRequest request
    ) {
        // Если нужно использовать uuid, передаём его в сервис, если нет – игнорируем
        return ResponseEntity.ok(
                service.create(
                        uuid,
                        request.getModuleId(),
                        request.getAccountId(),
                        request.getTemplateId(),
                        request.getPromtId(),
                        request.getActive() != null ? request.getActive() : true
                )
        );
    }

    @Operation(
            summary = "Обновить существующую привязку",
            description = "Обновляет параметры привязки по её ID."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Привязка успешно обновлена",
            content = @Content(schema = @Schema(implementation = BindingResponse.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Ошибка валидации или бизнес-логики"
    )
    @PutMapping("/{id}")
    public ResponseEntity<BindingResponse> update(
            @Parameter(hidden = true)
            @RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
            @PathVariable Long id,
            @RequestBody BindingUpdateRequest request
    ) {
        return ResponseEntity.ok(
                service.update(
                        uuid,
                        id,
                        request.getModuleId(),
                        request.getAccountId(),
                        request.getTemplateId(),
                        request.getPromtId(),
                        request.getActive()
                )
        );
    }


    @Operation(
            summary = "Получить все привязки для текущего пользователя",
            description = "Возвращает список всех привязок, принадлежащих пользователю (по UUID из заголовка). " +
                    "Возвращаются DTO с расширенной информацией (имена модулей, аккаунтов, шаблонов, промтов)."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Список DTO привязок",
            content = @Content(
                    array = @ArraySchema(schema = @Schema(implementation = BindingResponse.class))
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Ошибка при получении"
    )
    @GetMapping("/user")
    public ResponseEntity<List<BindingResponse>> getByUser(
            @Parameter(hidden = true)
            @RequestHeader(HeaderName.UUID_USER_HEADER) String uuid) {
        return ResponseEntity.ok(service.getBindingsForUser(uuid));
    }

    @Operation(
            summary = "Удалить привязку",
            description = "Удаляет привязку по её ID."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Привязка удалена"
    )
    @ApiResponse(
            responseCode = "400",
            description = "Привязка не найдена"
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(hidden = true)
            @RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
                                       @PathVariable Long id) {
        service.delete(uuid, id);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Активировать привязку",
            description = "Устанавливает статус активности = true для указанной привязки."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Привязка активирована",
            content = @Content(schema = @Schema(implementation = AccountTemplateBinding.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Привязка не найдена"
    )
    @PostMapping("/{id}/activate")
    public ResponseEntity<BindingResponse> activate(
            @Parameter(hidden = true)
            @RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
                                                           @PathVariable Long id) {
        return ResponseEntity.ok(service.activate(uuid, id));
    }

    @Operation(
            summary = "Деактивировать привязку",
            description = "Устанавливает статус активности = false для указанной привязки."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Привязка деактивирована",
            content = @Content(schema = @Schema(implementation = AccountTemplateBinding.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Привязка не найдена"
    )
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<BindingResponse> deactivate(
            @Parameter(hidden = true)
            @RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
                                                             @PathVariable Long id) {
        return ResponseEntity.ok(service.deactivate(uuid, id));
    }

    @Operation(
            summary = "Установить статус активности",
            description = "Устанавливает произвольный статус активности (true/false) для привязки."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Статус обновлён",
            content = @Content(schema = @Schema(implementation = AccountTemplateBinding.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Привязка не найдена"
    )
    @PostMapping("/{id}/active")
    public ResponseEntity<BindingResponse> setActive(
            @Parameter(hidden = true)
            @RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
            @PathVariable Long id,
            @RequestParam boolean active
    ) {
        return ResponseEntity.ok(service.setActive(uuid, id, active));
    }

    @Operation(
            summary = "Получить все привязки для пользователя и модуля",
            description = "Возвращает список привязок для конкретного пользователя (по UUID) и модуля. " +
                    "В случае ошибки возвращает текстовое сообщение."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Список DTO привязок (BindingResponse)",
            content = @Content(
                    array = @ArraySchema(schema = @Schema(implementation = BindingResponse.class))
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Ошибка (например, пользователь или модуль не найдены) – возвращает текст ошибки"
    )
    @GetMapping("module/{moduleId}")
    public ResponseEntity<?> getAllBindingsForUser(
            @Parameter(hidden = true)
            @RequestHeader(HeaderName.UUID_USER_HEADER) String uuid, @PathVariable Long moduleId) {
        try {
            List<BindingResponse> result = service.getBindingsForUserAndModule(uuid, moduleId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.debug("Error getting bindings for user {} and module {}", uuid, moduleId, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}