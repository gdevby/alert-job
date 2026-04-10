package by.gdev.alert.job.notification.service.ai.credential;

import by.gdev.alert.job.notification.model.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserCredentialService {

    private final CredentialClient credentialClient;
    private final EncryptionService encryptionService;

    public DecryptedCredential getUserCredentials(AiNotificationPayload payload) {

        String userUuid = payload.getUser().getUuid();
        Long moduleId = payload.getModule().getId();
        Long siteId = payload.getOrder().getSourceSite().getId();

        UserCredentialEncrypted encrypted = credentialClient.getEncryptedCredentials(
                userUuid,
                siteId,
                moduleId
        );

        String decryptedPassword = encryptionService.decrypt(encrypted.getPasswordEncrypted());

        return new DecryptedCredential(
                encrypted.getLogin(),
                decryptedPassword
        );
    }
}
