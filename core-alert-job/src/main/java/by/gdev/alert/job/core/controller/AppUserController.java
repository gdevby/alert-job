package by.gdev.alert.job.core.controller;

import by.gdev.alert.job.core.model.AppUserDTO;
import by.gdev.alert.job.core.model.db.AppUser;
import by.gdev.alert.job.core.service.AppUserService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Users", description = "Управление дополнительной информацией о пользователях")
@Hidden
public class AppUserController {

    private final AppUserService userService;


    @Operation(
            summary = "Получить пользователя по UUID",
            description = "Возвращает данные пользователя (UUID и email) по указанному UUID."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Пользователь найден",
            content = @Content(schema = @Schema(implementation = AppUserDTO.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "Пользователь не найден"
    )
    @ApiResponse(
            responseCode = "400",
            description = "Ошибка при получении"
    )
    @GetMapping("/{uuid}")
    public ResponseEntity<?> getUserByUuid(@PathVariable String uuid) {
        try {
            AppUser user = userService.findByUuid(uuid)
                    .orElse(null);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            AppUserDTO dto = new AppUserDTO();
            dto.setUuid(user.getUuid());
            dto.setEmail(user.getEmail());
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.warn("Ошибка при получении пользователя в CORE {}", uuid, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}

