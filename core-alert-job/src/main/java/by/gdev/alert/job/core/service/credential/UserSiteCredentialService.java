package by.gdev.alert.job.core.service.credential;

import by.gdev.alert.job.core.model.UserCredentialEncrypted;
import by.gdev.alert.job.core.model.db.UserSiteCredential;
import by.gdev.alert.job.core.repository.AppUserRepository;
import by.gdev.alert.job.core.repository.OrderModulesRepository;
import by.gdev.alert.job.core.repository.UserSiteCredentialRepository;
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

    private final UserSiteCredentialRepository repository;
    private final AppUserRepository userRepository;
    private final OrderModulesRepository moduleRepository;
    private final EncryptionService encryptionService;

    public UserSiteCredential createOrUpdateCredential(
            String userUuid,
            Long siteId,
            Long moduleId,
            String login,
            String rawPassword
    ) {
        // --- ВАЛИДАЦИЯ ПОЛЬЗОВАТЕЛЯ ---
        if (userRepository.findByUuid(userUuid).isEmpty()) {
            throw new IllegalArgumentException("User with uuid " + userUuid + " does not exist");
        }

        // --- ВАЛИДАЦИЯ МОДУЛЯ ---
        if (!moduleRepository.existsById(moduleId)) {
            throw new IllegalArgumentException("Module with id " + moduleId + " does not exist");
        }

        // --- ШИФРОВАНИЕ ---
        String encryptedPassword = encryptionService.encrypt(rawPassword);

        // --- СОЗДАНИЕ ИЛИ ОБНОВЛЕНИЕ ---
        return repository.findByUserUuidAndSiteIdAndModuleId(userUuid, siteId, moduleId)
                .map(existing -> {
                    existing.setLogin(login);
                    existing.setPasswordEncrypted(encryptedPassword);
                    existing.setUpdatedAt(LocalDateTime.now());

                    UserSiteCredential saved = repository.save(existing);

                    log.debug(
                            "Updated credentials: userUuid={}, siteId={}, moduleId={}, login={}",
                            userUuid, siteId, moduleId, login
                    );

                    return saved;
                })
                .orElseGet(() -> {
                    UserSiteCredential credential = UserSiteCredential.builder()
                            .userUuid(userUuid)
                            .siteId(siteId)
                            .moduleId(moduleId)
                            .login(login)
                            .passwordEncrypted(encryptedPassword)
                            .build();

                    UserSiteCredential saved = repository.save(credential);

                    log.debug(
                            "Created new credentials: userUuid={}, siteId={}, moduleId={}, login={}",
                            userUuid, siteId, moduleId, login
                    );

                    return saved;
                });
    }

    public Optional<UserCredentialEncrypted> getEncryptedCredential(
            String userUuid,
            Long siteId,
            Long moduleId
    ) {
        return repository.findByUserUuidAndSiteIdAndModuleId(userUuid, siteId, moduleId)
                .map(cred -> {

                    log.info(
                            "Получены учётные данные: userUuid={}, siteId={}, moduleId={}, login={}",
                            userUuid, siteId, moduleId, cred.getLogin()
                    );

                    UserCredentialEncrypted dto = new UserCredentialEncrypted();
                    dto.setLogin(cred.getLogin());
                    dto.setPasswordEncrypted(cred.getPasswordEncrypted());
                    return dto;
                });
    }

    public Optional<UserSiteCredential> getCredential(String userUuid, Long siteId, Long moduleId) {
        return repository.findByUserUuidAndSiteIdAndModuleId(userUuid, siteId, moduleId);
    }

    public List<UserSiteCredential> getCredentialsForUser(String userUuid) {
        return repository.findByUserUuid(userUuid);
    }

    public void delete(Long id) {
        repository.deleteById(id);
        log.debug("Deleted credentials with id={}", id);
    }
}
