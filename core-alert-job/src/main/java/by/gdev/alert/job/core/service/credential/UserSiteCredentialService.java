package by.gdev.alert.job.core.service.credential;

import by.gdev.alert.job.core.exeption.ai.AccessDeniedException;
import by.gdev.alert.job.core.exeption.ai.credential.CredentialNotFoundException;
import by.gdev.alert.job.core.model.UserCredentialEncrypted;
import by.gdev.alert.job.core.model.db.ai.UserSiteCredential;
import by.gdev.alert.job.core.repository.ai.UserSiteCredentialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSiteCredentialService {

    private final UserSiteCredentialRepository userSiteCredentialRepository;
    private final EncryptionService encryptionService;

    public UserSiteCredential createOrUpdateCredential(
            String name,
            String userUuid,
            Long siteId,
            String login,
            String rawPassword
    ) {
        String encryptedPassword = encryptionService.encrypt(rawPassword);

        // Ищем по userUuid, siteId и name
        Optional<UserSiteCredential> existingOpt = userSiteCredentialRepository
                .findByUserUuidAndSiteIdAndName(userUuid, siteId, name);

        if (existingOpt.isPresent()) {
            UserSiteCredential existing = existingOpt.get();
            existing.setLogin(login);
            existing.setPasswordEncrypted(encryptedPassword);
            existing.setUpdatedAt(LocalDateTime.now());
            // name не меняем, так как оно является частью ключа
            UserSiteCredential saved = userSiteCredentialRepository.save(existing);
            log.debug("Обновлены учётные данные: userUuid={}, siteId={}, name={}, login={}",
                    userUuid, siteId, name, login);
            return saved;
        } else {
            UserSiteCredential newCredential = UserSiteCredential.builder()
                    .name(name)
                    .userUuid(userUuid)
                    .siteId(siteId)
                    .login(login)
                    .passwordEncrypted(encryptedPassword)
                    .build();
            UserSiteCredential saved = userSiteCredentialRepository.save(newCredential);
            log.debug("Созданы новые учётные данные: userUuid={}, siteId={}, name={}, login={}",
                    userUuid, siteId, name, login);
            return saved;
        }
    }

    public List<UserSiteCredential> getByUserUuid(String uuid) {
        return userSiteCredentialRepository.findByUserUuid(uuid);
    }

    public List<UserSiteCredential> getCredentialsForUser(String userUuid) {
        return userSiteCredentialRepository.findByUserUuid(userUuid);
    }

    public void delete(Long id) {
        userSiteCredentialRepository.deleteById(id);
        log.debug("Deleted credentials with id={}", id);
    }

    public UserCredentialEncrypted getEncryptedById(String uuid, Long credentialId) {
        UserSiteCredential cred = userSiteCredentialRepository.findById(credentialId)
                .orElseThrow(() -> new CredentialNotFoundException("Учетные данные не найдены: " + credentialId));

        if (!uuid.equals(cred.getUserUuid())) {
            throw new AccessDeniedException("У вас нет доступа к этим учётным данным");
        }

        UserCredentialEncrypted dto = new UserCredentialEncrypted();
        dto.setLogin(cred.getLogin());
        dto.setPasswordEncrypted(cred.getPasswordEncrypted());
        dto.setName(cred.getName());
        return dto;
    }
}