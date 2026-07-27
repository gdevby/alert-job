package by.gdev.alert.job.core.controller;

import by.gdev.alert.job.core.model.ai.NotificationTypeDto;
import by.gdev.alert.job.core.service.OrderModulesService;
import by.gdev.common.model.NotificationTypeEnum;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/modules")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Modules", description = "Управление модулями")
public class OrderModulesController {

    private final OrderModulesService orderModulesService;

    @Operation(
            summary = "Получить список пользователей с автоответом",
            description = "Возвращает UUID пользователей, у которых включён автоответ (служебный эндпоинт)"
    )
    @Hidden
    @GetMapping("/auto-reply/users")
    public List<String> getUsersWithAutoReplyEnabled() {
        return orderModulesService.findDistinctUserUuidsWithAutoReplyEnabled();
    }

    @Operation(
            summary = "Получить список типов уведомлений",
            description = "Возвращает все доступные типы уведомлений с кодами и описаниями. " +
                    "Используется при создании/обновлении биндинга для выбора способа уведомления."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Список типов уведомлений успешно получен",
            content = @Content(
                    array = @ArraySchema(
                            schema = @Schema(implementation = NotificationTypeDto.class)
                    )
            )
    )
    @ApiResponse(
            responseCode = "500",
            description = "Внутренняя ошибка сервера"
    )
    @GetMapping("/notification-types")
    public List<NotificationTypeDto> getNotificationTypes() {
        return Arrays.stream(NotificationTypeEnum.values())
                .map(type -> new NotificationTypeDto(type.name(), type.getDescription()))
                .collect(Collectors.toList());
    }
}