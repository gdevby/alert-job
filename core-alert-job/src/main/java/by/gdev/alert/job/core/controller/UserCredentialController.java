package by.gdev.alert.job.core.controller;

import by.gdev.alert.job.core.model.UserCredentialEncrypted;
import by.gdev.alert.job.core.model.credential.dto.UserCredentialRequest;
import by.gdev.alert.job.core.model.db.ai.UserSiteCredential;
import by.gdev.alert.job.core.model.credential.dto.UserSiteCredentialShortResponse;
import by.gdev.alert.job.core.service.credential.UserSiteCredentialService;
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
    @GetMapping("/user/{uuid}/all")
    public ResponseEntity<?> getAllUserCredentials(@PathVariable String uuid) {
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
    public UserSiteCredential createOrUpdate(@RequestBody UserCredentialRequest request) {
        return credentialService.createOrUpdateCredential(
                request.getName(),
                request.getUserUuid(),
                request.getSiteId(),
                request.getModuleId(),
                request.getLogin(),
                request.getPassword()
        );
    }

    @Operation(
            summary = "Получить зашифрованные учётные данные",
            description = "Возвращает логин и зашифрованный пароль для указанных пользователя, сайта и модуля."
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
    @GetMapping("/get-encrypted")
    public ResponseEntity<UserCredentialEncrypted> getEncrypted(
            @RequestParam String userUuid,
            @RequestParam Long siteId,
            @RequestParam Long moduleId
    ) {
        return credentialService.getEncryptedCredential(userUuid, siteId, moduleId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
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
    public ResponseEntity<UserCredentialEncrypted> getEncryptedById(@RequestParam Long credentialId) {
        UserSiteCredential cred = credentialService.getById(credentialId);
        // Если cred == null, нужно вернуть 404, иначе NullPointerException
        if (cred == null) {
            return ResponseEntity.notFound().build();
        }
        UserCredentialEncrypted dto = new UserCredentialEncrypted();
        dto.setLogin(cred.getLogin());
        dto.setPasswordEncrypted(cred.getPasswordEncrypted());
        return ResponseEntity.ok(dto);
    }
}