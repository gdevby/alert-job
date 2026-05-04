package by.gdev.alert.job.core.controller;

import by.gdev.alert.job.core.model.UserCredentialEncrypted;
import by.gdev.alert.job.core.model.UserCredentialRequest;
import by.gdev.alert.job.core.model.db.UserSiteCredential;
import by.gdev.alert.job.core.service.credential.UserSiteCredentialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/credentials")
@RequiredArgsConstructor
public class UserCredentialController {

    private final UserSiteCredentialService credentialService;

    @PostMapping("/create-or-update")
    public UserSiteCredential createOrUpdate(@RequestBody UserCredentialRequest request) {
        return credentialService.createOrUpdateCredential(
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

}
