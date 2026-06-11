package by.gdev.alert.job.core.controller;

import by.gdev.alert.job.core.model.UserCredentialEncrypted;
import by.gdev.alert.job.core.model.credential.dto.UserCredentialRequest;
import by.gdev.alert.job.core.model.db.ai.UserSiteCredential;
import by.gdev.alert.job.core.model.credential.dto.UserSiteCredentialShortResponse;
import by.gdev.alert.job.core.service.credential.UserSiteCredentialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/credentials")
@RequiredArgsConstructor
public class UserCredentialController {

    private final UserSiteCredentialService credentialService;

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
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

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

    @GetMapping("/get-encrypted-by-id")
    public ResponseEntity<UserCredentialEncrypted> getEncryptedById(@RequestParam Long credentialId) {
        UserSiteCredential cred = credentialService.getById(credentialId);
        UserCredentialEncrypted dto = new UserCredentialEncrypted();
        dto.setLogin(cred.getLogin());
        dto.setPasswordEncrypted(cred.getPasswordEncrypted());
        return ResponseEntity.ok(dto);

    }
}