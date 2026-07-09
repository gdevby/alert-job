package by.gdev.alert.job.core.service.credential;

import by.gdev.alert.job.core.client.NotificationClient;
import by.gdev.alert.job.core.exeption.ai.AccessDeniedException;
import by.gdev.alert.job.core.exeption.ai.InvalidSiteIdException;
import by.gdev.alert.job.core.exeption.ai.credential.CredentialNotFoundException;
import by.gdev.alert.job.core.model.UserCredentialEncrypted;
import by.gdev.alert.job.core.model.db.ai.AccountTemplateBinding;
import by.gdev.alert.job.core.model.db.ai.UserSiteCredential;
import by.gdev.alert.job.core.repository.ai.AccountTemplateBindingRepository;
import by.gdev.alert.job.core.repository.ai.UserSiteCredentialRepository;
import by.gdev.common.model.SiteName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSiteCredentialService {

    private final AccountTemplateBindingRepository accountTemplateBindingRepository;
    private final UserSiteCredentialRepository userSiteCredentialRepository;
    private final EncryptionService encryptionService;
    private final NotificationClient notificationClient;

    public UserSiteCredential createOrUpdateCredential(
            String name,
            String userUuid,
            Long siteId,
            String login,
            String rawPassword
    ) {
        // Получаем список поддерживаемых сайтов
        List<SiteName> supportedSites = notificationClient.getSupportedSites();
        // Проверяем, что siteId есть в списке
        boolean valid = supportedSites.stream()
                .anyMatch(s -> s.getId() == siteId);
        if (!valid) {
            String supportedIds = supportedSites.stream()
                    .map(s -> s.getId() + "(" + s.name() + ")")
                    .collect(Collectors.joining(", "));
            throw new InvalidSiteIdException(
                    "Некорректный siteId: " + siteId +
                            ". Доступные ID: " + supportedIds
            );
        }

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

    /**
     * Удаляет учётные данные по ID, если они принадлежат пользователю с указанным UUID.
     *
     * @param uuid UUID пользователя из заголовка
     * @param id   ID учётной записи
     * @throws CredentialNotFoundException если запись не найдена
     * @throws AccessDeniedException       если запись не принадлежит пользователю
     */
    @Transactional
    public void delete(String uuid, Long id) {
        // Находим учётную запись
        UserSiteCredential cred = userSiteCredentialRepository.findById(id)
                .orElseThrow(() -> new CredentialNotFoundException("Учетные данные не найдены: " + id));

        // Проверяем, что она принадлежит пользователю
        if (!uuid.equals(cred.getUserUuid())) {
            throw new AccessDeniedException("У вас нет доступа к этим учётным данным");
        }

        // Находим все биндинги, ссылающиеся на этот аккаунт
        List<AccountTemplateBinding> bindings = accountTemplateBindingRepository.findByAccountId(id);
        if (!bindings.isEmpty()) {
            accountTemplateBindingRepository.deleteAll(bindings);
            log.debug("Удалено {} биндингов для аккаунта {}", bindings.size(), id);
        }

        // Удаляем саму учётную запись
        userSiteCredentialRepository.delete(cred);
        log.debug("Удалена учётная запись id={}", id);
    }
}