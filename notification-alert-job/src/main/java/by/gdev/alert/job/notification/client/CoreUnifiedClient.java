package by.gdev.alert.job.notification.client;

import by.gdev.alert.job.notification.model.dto.AppUserDTO;
import by.gdev.alert.job.notification.model.dto.ModuleSiteDto;
import by.gdev.alert.job.notification.model.dto.UserCredentialEncrypted;
import by.gdev.common.model.HeaderName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class CoreUnifiedClient {

    private final WebClient webClient;

    @Value("${core.service.url}")
    private String coreUrl;

    // ---------- Credentials ----------
    public UserCredentialEncrypted getEncryptedCredentials(
            String userUuid,
            Long siteId,
            Long moduleId
    ) {
        return webClient.get()
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
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/credentials/get-encrypted")
                        .queryParam("userUuid", userUuid)
                        .queryParam("siteId", siteId)
                        .queryParam("moduleId", moduleId)
                        .build())
                .retrieve()
                .bodyToMono(UserCredentialEncrypted.class);
    }

    public UserCredentialEncrypted getEncryptedCredentialsById(String uuid, Long credentialId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/credentials/get-encrypted-by-id")
                        .queryParam("credentialId", credentialId)
                        .build())
                .header(HeaderName.UUID_USER_HEADER, uuid)
                .retrieve()
                .bodyToMono(UserCredentialEncrypted.class)
                .block();
    }

    // Auto-reply users
    public Mono<List<String>> getUsersWithAutoReplyEnabled() {
        return webClient.get()
                .uri(coreUrl + "/api/modules/auto-reply/users")
                .retrieve()
                .bodyToMono(String[].class)
                .map(List::of)
                .doOnError(e -> log.error("Ошибка получения пользователей с автоответом", e))
                .onErrorReturn(Collections.emptyList());
    }

    //  User sites with auto-reply
    public List<ModuleSiteDto> getAutoReplyEnabledModules(String userUuid) {
        try {
            return webClient.get()
                    .uri(coreUrl + "/api/users/" + userUuid + "/auto-reply-modules")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<ModuleSiteDto>>() {})
                    .block();
        } catch (Exception e) {
            log.error("Ошибка получения модулей с автоответом для пользователя {}", userUuid, e);
            return Collections.emptyList();
        }
    }

}
