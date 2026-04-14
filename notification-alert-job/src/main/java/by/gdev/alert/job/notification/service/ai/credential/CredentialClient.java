package by.gdev.alert.job.notification.service.ai.credential;

import by.gdev.alert.job.notification.model.dto.UserCredentialEncrypted;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class CredentialClient {

    private final CoreWebClient coreWebClient;

    public UserCredentialEncrypted getEncryptedCredentials(
            String userUuid,
            Long siteId,
            Long moduleId
    ) {
        return coreWebClient.getClient()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/credentials/get-encrypted")
                        .queryParam("userUuid", userUuid)
                        .queryParam("siteId", siteId)
                        .queryParam("moduleId", moduleId)
                        .build())
                .retrieve()
                .bodyToMono(UserCredentialEncrypted.class)
                .block();
    }


    public Mono<UserCredentialEncrypted> getMonoEncryptedCredentials(
            String userUuid,
            Long siteId,
            Long moduleId
    ) {
        return coreWebClient.getClient()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/credentials/get-encrypted")
                        .queryParam("userUuid", userUuid)
                        .queryParam("siteId", siteId)
                        .queryParam("moduleId", moduleId)
                        .build())
                .retrieve()
                .bodyToMono(UserCredentialEncrypted.class);
    }
}
