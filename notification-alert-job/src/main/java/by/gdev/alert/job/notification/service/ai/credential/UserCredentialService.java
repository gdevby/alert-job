package by.gdev.alert.job.notification.service.ai.credential;

import by.gdev.alert.job.notification.model.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

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


    public DecryptedCredential getUserCredentialsBlocking(AiNotificationPayload payload) {

        String userUuid = payload.getUser().getUuid();
        Long moduleId = payload.getModule().getId();
        Long siteId = payload.getOrder().getSourceSite().getSource();

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

    public Mono<DecryptedCredential> getMonoUserCredentials(AiNotificationPayload payload) {

        String userUuid = payload.getUser().getUuid(); //uuid пользователя
        Long moduleId = payload.getModule().getId(); //модуль
        Long siteId = payload.getOrder().getSourceSite().getSource(); //сайт

        return credentialClient.getMonoEncryptedCredentials(userUuid, siteId, moduleId)
                .map(encrypted -> {
                    String decryptedPassword = encryptionService.decrypt(encrypted.getPasswordEncrypted());
                    return new DecryptedCredential(
                            encrypted.getLogin(),
                            decryptedPassword
                    );
                });
    }
}
