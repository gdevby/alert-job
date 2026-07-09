package by.gdev.alert.job.core.controller;

import by.gdev.alert.job.core.model.UserCredentialEncrypted;
import by.gdev.alert.job.core.model.credential.dto.UserCredentialRequest;
import by.gdev.alert.job.core.model.db.ai.UserSiteCredential;
import by.gdev.alert.job.core.model.credential.dto.UserSiteCredentialShortResponse;
import by.gdev.alert.job.core.service.credential.UserSiteCredentialService;
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

@RestController
@RequestMapping("/api/credentials")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Credentials", description = "Управление учётными данными пользователей для сайтов/модулей")
public class UserCredentialController {

    private final UserSiteCredentialService credentialService;

    @Operation(
            summary = "Получить все учётные данные пользователя",
            description = "Возвращает список всех учётных данных для указанного пользователя (по UUID). " +
                    "Возвращается сокращённая информация (ID, имя, логин, дата создания)."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Список учётных данных",
            content = @Content(schema = @Schema(implementation = UserSiteCredentialShortResponse.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Ошибка (например, пользователь не найден)"
    )
    @GetMapping("/user/all")
    public ResponseEntity<?> getAllUserCredentials(
            @Parameter(hidden = true)
            @RequestHeader(HeaderName.UUID_USER_HEADER) String uuid) {
        try {
            var creds = credentialService.getByUserUuid(uuid);
            var result = creds.stream().map(c -> {
                UserSiteCredentialShortResponse dto = new UserSiteCredentialShortResponse();
                dto.setId(c.getId());
                dto.setName(c.getName());
                dto.setLogin(c.getLogin());
                dto.setCreatedAt(
                        c.getCreatedAt() != null ? c.getCreatedAt().toString() : null
                );
                return dto;
            }).toList();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error getting credentials for user {}", uuid, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(
            summary = "Создать или обновить учётные данные",
            description = "Создаёт новую запись учётных данных или обновляет существующую. " +
                    "Возвращает сохранённый объект UserSiteCredential."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Учётные данные сохранены",
            content = @Content(schema = @Schema(implementation = UserSiteCredential.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Ошибка валидации или бизнес-логики"
    )
    @PostMapping("/create-or-update")
    public UserSiteCredential createOrUpdate(
            @Parameter(hidden = true)
            @RequestHeader(HeaderName.UUID_USER_HEADER) String uuid, @RequestBody UserCredentialRequest request) {
        return credentialService.createOrUpdateCredential(
                request.getName(),
                uuid,
                request.getSiteId(),
                request.getLogin(),
                request.getPassword()
        );
    }

    @Operation(
            summary = "Получить зашифрованные учётные данные по ID",
            description = "Возвращает логин и зашифрованный пароль для указанного ID учётной записи."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Учётные данные найдены",
            content = @Content(schema = @Schema(implementation = UserCredentialEncrypted.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "Учётные данные не найдены"
    )
    @GetMapping("/get-encrypted-by-id")
    public ResponseEntity<UserCredentialEncrypted> getEncryptedById(
            @Parameter(hidden = true)
            @RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
            @RequestParam Long credentialId) {

        UserCredentialEncrypted dto = credentialService.getEncryptedById(uuid, credentialId);
        return ResponseEntity.ok(dto);
    }

    @Operation(
            summary = "Удалить учётные данные по ID",
            description = "Удаляет запись учётных данных. Можно удалять только свои записи."
    )
    @ApiResponse(
            responseCode = "204",
            description = "Учётные данные успешно удалены"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Учётные данные не найдены"
    )
    @ApiResponse(
            responseCode = "403",
            description = "У вас нет доступа к этим учётным данным"
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCredential(
            @Parameter(hidden = true)
            @RequestHeader(HeaderName.UUID_USER_HEADER) String uuid,
            @PathVariable Long id) {
        credentialService.delete(uuid, id);
        return ResponseEntity.noContent().build();
    }

}