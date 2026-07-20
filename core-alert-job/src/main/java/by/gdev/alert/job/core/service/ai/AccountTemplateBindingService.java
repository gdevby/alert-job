package by.gdev.alert.job.core.service.ai;

import by.gdev.alert.job.core.client.LlmClient;
import by.gdev.alert.job.core.exeption.ai.*;
import by.gdev.alert.job.core.exeption.ai.binding.BindingAlreadyExistsException;
import by.gdev.alert.job.core.exeption.ai.binding.BindingNotFoundException;
import by.gdev.alert.job.core.model.binding.dto.BindingResponse;
import by.gdev.alert.job.core.model.db.OrderModules;
import by.gdev.alert.job.core.model.db.ai.AccountTemplateBinding;
import by.gdev.alert.job.core.model.db.ai.UserSiteCredential;
import by.gdev.alert.job.core.model.promt.dto.PromtResponse;
import by.gdev.alert.job.core.model.template.dto.TemplateResponse;
import by.gdev.alert.job.core.repository.OrderModulesRepository;
import by.gdev.alert.job.core.repository.ai.AccountTemplateBindingRepository;
import by.gdev.alert.job.core.repository.ai.UserSiteCredentialRepository;
import by.gdev.common.model.NotificationTypeEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Comparator;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountTemplateBindingService {

    private final OrderModulesRepository orderModulesRepository;
    private final AccountTemplateBindingRepository accountTemplateBindingRepository;
    private final UserSiteCredentialRepository userSiteCredentialRepository;
    private final LlmClient llmClient;

    @Transactional
    public BindingResponse create(String uuid, Long moduleId, Long accountId,
                                  Long templateId, Long promtId, boolean active, NotificationTypeEnum notificationType) {

        if (!orderModulesRepository.existsById(moduleId)) {
            throw new OrderModuleNotFoundException("Модуль не найден с ид: " + moduleId);
        }

        // Проверка дубликатов по moduleId, accountId, templateId, promtId
        if (accountTemplateBindingRepository.existsByModuleIdAndAccountIdAndTemplateIdAndPromtId(
                moduleId, accountId, templateId, promtId)) {
            throw new BindingAlreadyExistsException(
                    "Связка с таким модулем, аккаунтом, шаблоном и промтом уже существует"
            );
        }

        // проверка существования шаблона в LLM
        if (!llmClient.templateExists(templateId, uuid)) {
            throw new TemplateNotFoundException("Шаблон не найден в модуле LLM: " + templateId);
        }

        // проверка существования промта в LLM
        if (!llmClient.promtExists(promtId, uuid)) {
            throw new TemplateNotFoundException("Промт не найден в модуле LLM: " + promtId);
        }

        if (!userSiteCredentialRepository.existsById(accountId)) {
            throw new UserCredentialNotFoundException("Учетные данные не найдены с ид: " + accountId);
        }
        checkBindingExistsForSite(moduleId, accountId, null);

        AccountTemplateBinding b = AccountTemplateBinding.builder()
                .moduleId(moduleId)
                .accountId(accountId)
                .templateId(templateId)
                .promtId(promtId)
                .active(active)
                .userUuid(uuid)
                .notificationType(notificationType != null ? notificationType : NotificationTypeEnum.NONE)
                .build();

        AccountTemplateBinding saved = accountTemplateBindingRepository.save(b);
        return convertToBindingResponse(uuid, saved);
    }

    @Transactional
    public BindingResponse update(String uuid, Long id, Long moduleId,
                                  Long accountId, Long templateId, Long promtId, boolean active, NotificationTypeEnum notificationType) {
        if (!orderModulesRepository.existsById(moduleId)) {
            throw new OrderModuleNotFoundException("Модуль не найден с ид: " + moduleId);
        }

        AccountTemplateBinding b = accountTemplateBindingRepository.findById(id)
                .orElseThrow(() -> new BindingNotFoundException("Связка аккаунта и шаблона не найдена: " + id));

        if (!uuid.equals(b.getUserUuid())) {
            throw new AccessDeniedException("Эта привязка не принадлежит текущему пользователю");
        }

        // Проверка дубликата (кроме текущего)
        boolean exists = accountTemplateBindingRepository.existsByModuleIdAndAccountIdAndTemplateIdAndPromtId(moduleId, accountId, templateId, promtId);
        if (exists && !(b.getModuleId().equals(moduleId)
                && b.getAccountId().equals(accountId)
                && b.getTemplateId().equals(templateId))
                && b.getPromtId().equals(promtId)) {
            throw new BindingAlreadyExistsException("Связка аккаунта и шаблона существует");
        }

        // Проверка шаблона
        if (!llmClient.templateExists(templateId, uuid)) {
            throw new TemplateNotFoundException("Шаблон не найден в модуле LLM: " + templateId);
        }

        //Проверка промта
        if (!llmClient.promtExists(promtId, uuid)) {
            throw new TemplateNotFoundException("Промт не найден в модуле LLM: " + promtId);
        }

        if (!userSiteCredentialRepository.existsById(accountId)) {
            throw new UserCredentialNotFoundException("Учетные данные не найдены с ид: " + accountId);
        }

        checkBindingExistsForSite(moduleId, accountId, id);

        // Обновление
        b.setModuleId(moduleId);
        b.setAccountId(accountId);
        b.setTemplateId(templateId);
        b.setPromtId(promtId);
        b.setActive(active);

        b.setNotificationType(notificationType != null
                ? notificationType
                : NotificationTypeEnum.NONE);

        AccountTemplateBinding saved = accountTemplateBindingRepository.save(b);
        return convertToBindingResponse(uuid, saved);
    }

    @Transactional
    public void delete(String uuid, Long id) {
        AccountTemplateBinding b = accountTemplateBindingRepository.findById(id)
                .orElseThrow(() -> new BindingNotFoundException("Связка не найдена: " + id));

        // Проверка по userUuid из биндинга
        if (!uuid.equals(b.getUserUuid())) {
            throw new AccessDeniedException("Эта привязка не принадлежит текущему пользователю");
        }
        accountTemplateBindingRepository.delete(b);
    }

    @Transactional
    public BindingResponse activate(String uuid, Long bindingId) {
        AccountTemplateBinding b = accountTemplateBindingRepository.findById(bindingId)
                .orElseThrow(() -> new BindingNotFoundException("Связка аккаунта и шаблона не найдена: " + bindingId));

        if (!uuid.equals(b.getUserUuid())) {
            throw new AccessDeniedException("Эта привязка не принадлежит текущему пользователю");
        }

        checkBindingExistsForSite(b.getModuleId(), b.getAccountId(), bindingId);
        b.setActive(true);
        AccountTemplateBinding saved = accountTemplateBindingRepository.save(b);
        return convertToBindingResponse(uuid, saved);
    }

    @Transactional
    public BindingResponse deactivate(String uuid, Long bindingId) {
        AccountTemplateBinding b = accountTemplateBindingRepository.findById(bindingId)
                .orElseThrow(() -> new BindingNotFoundException("Связка аккаунта и шаблона не найдена: " + bindingId));

        if (!uuid.equals(b.getUserUuid())) {
            throw new AccessDeniedException("Эта привязка не принадлежит текущему пользователю");
        }

        b.setActive(false);
        AccountTemplateBinding saved = accountTemplateBindingRepository.save(b);
        return convertToBindingResponse(uuid, saved);
    }

    @Transactional
    public BindingResponse setActive(String uuid, Long id, boolean active) {
        return active
                ? activate(uuid, id)
                : deactivate(uuid, id);
    }

    @Transactional(readOnly = true)
    public List<BindingResponse> getBindingsForUser(String userUuid) {
        List<UserSiteCredential> creds = userSiteCredentialRepository.findByUserUuid(userUuid);

        List<AccountTemplateBinding> bindings = creds.stream()
                .flatMap(c -> accountTemplateBindingRepository.findByAccountId(c.getId()).stream())
                .toList();

        return bindings.stream()
                .map(b -> convertToBindingResponse(userUuid, b)) // передаём userUuid
                .sorted(Comparator.comparing(BindingResponse::getCreatedAt))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BindingResponse> getBindingsForUserAndModule(String userUuid, Long moduleId) {
        List<UserSiteCredential> creds = userSiteCredentialRepository.findByUserUuid(userUuid);

        List<AccountTemplateBinding> bindings = creds.stream()
                .flatMap(c -> accountTemplateBindingRepository.findByAccountId(c.getId()).stream())
                .filter(b -> b.getModuleId().equals(moduleId))
                .toList();

        return bindings.stream()
                .map(b -> convertToBindingResponse(userUuid, b)) // передаём userUuid
                .sorted(Comparator.comparing(BindingResponse::getCreatedAt))
                .toList();
    }

    private BindingResponse convertToBindingResponse(String uuid, AccountTemplateBinding b) {
        BindingResponse dto = new BindingResponse();
        dto.setId(b.getId());
        dto.setModuleId(b.getModuleId());
        dto.setAccountId(b.getAccountId());
        dto.setTemplateId(b.getTemplateId());
        dto.setPromtId(b.getPromtId());
        dto.setActive(b.isActive());

        // Аккаунт пользователя для входа в парсер автоответов
        UserSiteCredential cred = userSiteCredentialRepository
                .findById(b.getAccountId())
                .orElse(null);
        dto.setAccountName(cred != null ? cred.getName() : "—");

        // Шаблон (через REST)
        TemplateResponse template = llmClient.getTemplate(b.getTemplateId(), uuid);
        dto.setTemplateName(template != null ? template.getName() : "—");

        // Модуль
        OrderModules module = orderModulesRepository.findById(b.getModuleId()).orElse(null);
        dto.setModuleName(module != null ? module.getName() : "—");

        // Промт (через REST)
        PromtResponse promt = llmClient.getPromt(b.getPromtId(), uuid);
        dto.setPromtName(promt != null ? promt.getName() : "—");

        // Дата создания
        dto.setCreatedAt(
                b.getCreatedAt() != null
                        ? b.getCreatedAt().toString()
                        : LocalDateTime.now().toString()
        );
        return dto;
    }

    /**
     * Проверяет, есть ли уже биндинг для этого модуля на этом сайте (кроме указанного).
     * Если есть — выбрасывает исключение.
     *
     * @param moduleId  ID модуля
     * @param accountId ID аккаунта (через него определяем siteId)
     * @param exceptId  ID биндинга, который нужно исключить из проверки (может быть null)
     */
    private void checkBindingExistsForSite(Long moduleId, Long accountId, Long exceptId) {
        // Берём аккаунт, узнаём его siteId
        UserSiteCredential account = userSiteCredentialRepository.findById(accountId)
                .orElseThrow(() -> new UserCredentialNotFoundException("Аккаунт не найден: " + accountId));

        Long siteId = account.getSiteId();

        // Находим ВСЕ аккаунты с этим siteId
        List<UserSiteCredential> accountsOnSite = userSiteCredentialRepository.findBySiteId(siteId);
        if (accountsOnSite.isEmpty()) {
            return;
        }

        // Берём их ID
        List<Long> accountIds = accountsOnSite.stream()
                .map(UserSiteCredential::getId)
                .collect(Collectors.toList());

        // Проверяем биндинги для этого модуля и этих аккаунтов
        List<AccountTemplateBinding> existingBindings = accountTemplateBindingRepository
                .findByModuleIdAndAccountIdIn(moduleId, accountIds);

        List<AccountTemplateBinding> otherBindings = existingBindings.stream()
                .filter(b -> exceptId == null || !b.getId().equals(exceptId))
                .toList();

        // Если есть ХОТЯ БЫ ОДИН биндинг (любой статус) — кидаем 409
        if (!otherBindings.isEmpty()) {
            throw new BindingAlreadyExistsException(
                    "Для модуля " + moduleId + " уже существует биндинг для сайта (siteId=" + siteId + ")"
            );
        }
    }

}
