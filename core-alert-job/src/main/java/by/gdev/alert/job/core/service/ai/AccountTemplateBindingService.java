package by.gdev.alert.job.core.service.ai;

import by.gdev.alert.job.core.client.LlmClient;
import by.gdev.alert.job.core.exeption.ai.*;
import by.gdev.alert.job.core.model.binding.dto.BindingResponse;
import by.gdev.alert.job.core.model.db.ai.AccountTemplateBinding;
import by.gdev.alert.job.core.model.db.ai.UserSiteCredential;
import by.gdev.alert.job.core.model.template.dto.TemplateResponse;
import by.gdev.alert.job.core.repository.OrderModulesRepository;
import by.gdev.alert.job.core.repository.ai.AccountTemplateBindingRepository;
import by.gdev.alert.job.core.repository.ai.UserSiteCredentialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class AccountTemplateBindingService {

    private final OrderModulesRepository orderModulesRepository;
    private final AccountTemplateBindingRepository accountTemplateBindingRepository;
    private final UserSiteCredentialRepository userSiteCredentialRepository;
    private final LlmClient llmClient;

    @Transactional
    public AccountTemplateBinding create(Long moduleId, Long accountId, Long templateId, boolean active) {

        if (!orderModulesRepository.existsById(moduleId)) {
            throw new OrderModuleNotFoundException("Модуль не найден с ид: " + moduleId);
        }

        // проверка дубликатов
        if (accountTemplateBindingRepository.existsByModuleIdAndAccountIdAndTemplateId(moduleId, accountId, templateId)) {
            throw new BindingAlreadyExistsException("Связка аккаунта и шаблона существует");
        }

        // проверка существования шаблона в LLM
        if (!llmClient.templateExists(templateId)) {
            throw new TemplateNotFoundException("Шаблон не найден в модуле LLM: " + templateId);
        }

        if (!userSiteCredentialRepository.existsById(accountId)) {
            throw new UserCredentialNotFoundException("Учетные данные не найдены с ид: " + accountId);
        }

        // Если создаём активный биндинг — выключаем остальные
        if (active) {
            deactivateOtherBindings(moduleId, null);
        }

        AccountTemplateBinding b = AccountTemplateBinding.builder()
                .moduleId(moduleId)
                .accountId(accountId)
                .templateId(templateId)
                .active(active)
                .build();

        return accountTemplateBindingRepository.save(b);
    }

    @Transactional
    public AccountTemplateBinding update(Long id, Long moduleId, Long accountId, Long templateId, boolean active) {

        if (!orderModulesRepository.existsById(moduleId)) {
            throw new OrderModuleNotFoundException("Модуль не найден с ид: " + moduleId);
        }

        if (!orderModulesRepository.existsById(moduleId)) {
            throw new OrderModuleNotFoundException("Модуль не найден с ид: " + moduleId);
        }

        AccountTemplateBinding b = accountTemplateBindingRepository.findById(id)
                .orElseThrow(() -> new BindingNotFoundException("Связка аккаунта и шаблона не найдена: " + id));

        // Проверка дубликата (кроме текущего)
        boolean exists = accountTemplateBindingRepository.existsByModuleIdAndAccountIdAndTemplateId(moduleId, accountId, templateId);
        if (exists && !(b.getModuleId().equals(moduleId)
                && b.getAccountId().equals(accountId)
                && b.getTemplateId().equals(templateId))) {
            throw new BindingAlreadyExistsException("Связка аккаунта и шаблона существует");
        }

        // Проверка шаблона
        if (!llmClient.templateExists(templateId)) {
            throw new TemplateNotFoundException("Шаблон не найден в модуле LLM: " + templateId);
        }

        if (!userSiteCredentialRepository.existsById(accountId)) {
            throw new UserCredentialNotFoundException("Учетные данные не найдены с ид: " + accountId);
        }

        // Если обновляем и ставим active=true — выключаем остальные
        if (active) {
            deactivateOtherBindings(moduleId, id);
        }

        // Обновление
        b.setModuleId(moduleId);
        b.setAccountId(accountId);
        b.setTemplateId(templateId);
        b.setActive(active);

        return accountTemplateBindingRepository.save(b);
    }


    @Transactional(readOnly = true)
    public List<AccountTemplateBinding> getByModule(Long moduleId) {
        return accountTemplateBindingRepository.findAllByModuleId(moduleId);
    }

    @Transactional
    public void delete(Long id) {
        accountTemplateBindingRepository.deleteById(id);
    }

    @Transactional
    public AccountTemplateBinding updateActive(Long bindingId, boolean active) {
        AccountTemplateBinding b = accountTemplateBindingRepository.findById(bindingId)
                .orElseThrow(() -> new BindingNotFoundException("Связка аккаунта и шаблона не найдена: " + bindingId));

        b.setActive(active);
        return accountTemplateBindingRepository.save(b);
    }

    @Transactional
    public AccountTemplateBinding activate(Long bindingId) {
        AccountTemplateBinding b = accountTemplateBindingRepository.findById(bindingId)
                .orElseThrow(() -> new BindingNotFoundException("Связка аккаунта и шаблона не найдена: " + bindingId));
        deactivateOtherBindings(b.getModuleId(), bindingId);
        b.setActive(true);
        return accountTemplateBindingRepository.save(b);
    }

    @Transactional
    public AccountTemplateBinding deactivate(Long bindingId) {
        AccountTemplateBinding b = accountTemplateBindingRepository.findById(bindingId)
                .orElseThrow(() -> new BindingNotFoundException("Связка аккаунта и шаблона не найдена: " + bindingId));

        b.setActive(false);
        return accountTemplateBindingRepository.save(b);
    }

    @Transactional
    public AccountTemplateBinding setActive(Long id, boolean active) {
        return active
                ? activate(id)
                : deactivate(id);
    }

    private void deactivateOtherBindings(Long moduleId, Long exceptId) {

        List<AccountTemplateBinding> all =
                accountTemplateBindingRepository.findAllByModuleId(moduleId);
        if (all.isEmpty()) {
            return; // нечего деактивировать
        }
        for (AccountTemplateBinding binding : all) {
            if (exceptId == null || !binding.getId().equals(exceptId)) {
                binding.setActive(false);
            }
        }
        accountTemplateBindingRepository.saveAll(all);
    }

    public List<BindingResponse> getBindingsForUser(String userUuid) {

        List<UserSiteCredential> creds = userSiteCredentialRepository.findByUserUuid(userUuid);

        List<AccountTemplateBinding> bindings = creds.stream()
                .flatMap(c -> accountTemplateBindingRepository.findByAccountId(c.getId()).stream())
                .toList();

        return bindings.stream()
                .map(b -> {
                    BindingResponse dto = new BindingResponse();
                    dto.setId(b.getId());

                    UserSiteCredential cred = userSiteCredentialRepository
                            .findById(b.getAccountId())
                            .orElse(null);
                    dto.setAccountName(cred != null ? cred.getName() : "—");

                    TemplateResponse template = llmClient.getTemplate(b.getTemplateId());
                    dto.setTemplateName(template != null ? template.getName() : "—");

                    dto.setCreatedAt(
                            b.getCreatedAt() != null
                                    ? b.getCreatedAt().toString()
                                    : LocalDateTime.now().toString()
                    );

                    dto.setActive(b.isActive());

                    return dto;
                })
                .sorted(Comparator.comparing(BindingResponse::getCreatedAt))
                .toList();
    }
}
