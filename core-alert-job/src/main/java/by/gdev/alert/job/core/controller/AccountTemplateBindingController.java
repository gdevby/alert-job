package by.gdev.alert.job.core.controller;

import by.gdev.alert.job.core.model.binding.dto.BindingResponse;
import by.gdev.alert.job.core.model.db.ai.AccountTemplateBinding;
import by.gdev.alert.job.core.service.ai.AccountTemplateBindingService;
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
    public ResponseEntity<AccountTemplateBinding> create(
            @RequestParam Long moduleId,
            @RequestParam Long accountId,
            @RequestParam Long templateId,
            @RequestParam(defaultValue = "true") boolean active
    ) {
        return ResponseEntity.ok(
                service.create(moduleId, accountId, templateId, active)
        );
    }

    @Operation(
            summary = "Обновить существующую привязку",
            description = "Изменяет параметры привязки по её ID."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Привязка обновлена",
            content = @Content(schema = @Schema(implementation = AccountTemplateBinding.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Ошибка валидации или привязка не найдена"
    )
    @PutMapping("/{id}")
    public ResponseEntity<AccountTemplateBinding> update(
            @PathVariable Long id,
            @RequestParam Long moduleId,
            @RequestParam Long accountId,
            @RequestParam Long templateId,
            @RequestParam boolean active
    ) {
        return ResponseEntity.ok(
                service.update(id, moduleId, accountId, templateId, active)
        );
    }

    @Operation(
            summary = "Получить все привязки для модуля",
            description = "Возвращает список всех привязок, относящихся к указанному модулю."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Список привязок",
            content = @Content(schema = @Schema(implementation = AccountTemplateBinding.class))
    )
    @GetMapping("/{moduleId}")
    public ResponseEntity<List<AccountTemplateBinding>> getByModule(@PathVariable Long moduleId) {
        return ResponseEntity.ok(service.getByModule(moduleId));
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
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
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
    public ResponseEntity<AccountTemplateBinding> activate(@PathVariable Long id) {
        return ResponseEntity.ok(service.activate(id));
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
    public ResponseEntity<AccountTemplateBinding> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(service.deactivate(id));
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
    public ResponseEntity<AccountTemplateBinding> setActive(
            @PathVariable Long id,
            @RequestParam boolean active
    ) {
        return ResponseEntity.ok(service.setActive(id, active));
    }

    @Operation(
            summary = "Получить все привязки для пользователя и модуля",
            description = "Возвращает список привязок для конкретного пользователя (по UUID) и модуля. " +
                    "В случае ошибки возвращает текстовое сообщение."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Список привязок (BindingResponse)",
            content = @Content(schema = @Schema(implementation = BindingResponse.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Ошибка (например, пользователь или модуль не найдены) – возвращает текст ошибки"
    )
    @GetMapping("/user/{uuid}/module/{moduleId}")
    public ResponseEntity<?> getAllBindingsForUser(@PathVariable String uuid, @PathVariable Long moduleId) {
        try {
            List<BindingResponse> result = service.getBindingsForUserAndModule(uuid, moduleId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.debug("Error getting bindings for user {} and module {}", uuid, moduleId, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}