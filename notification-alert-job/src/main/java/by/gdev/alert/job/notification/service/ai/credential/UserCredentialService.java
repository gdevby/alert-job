package by.gdev.alert.job.notification.service.ai.credential;

import by.gdev.alert.job.notification.client.CoreUnifiedClient;
import by.gdev.alert.job.notification.model.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserCredentialService {

    private final CoreUnifiedClient credentialClient;
    private final EncryptionService encryptionService;


    public DecryptedCredential getUserCredentialsBlocking(AiNotificationPayload payload) {

        Long credentialId = payload.getCredentialId();
        String uuid = payload.getUser().getUuid();

        UserCredentialEncrypted encrypted =
                credentialClient.getEncryptedCredentialsById(uuid, credentialId);

        String decryptedPassword = encryptionService.decrypt(encrypted.getPasswordEncrypted());

        return new DecryptedCredential(
                encrypted.getLogin(),
                decryptedPassword
        );
    }

}
