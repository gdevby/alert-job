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
    public BindingResponse create(String uuid, Long moduleId, Long accountId, Long templateId, Long promtId, boolean active) {

        if (!orderModulesRepository.existsById(moduleId)) {
            throw new OrderModuleNotFoundException("Модуль не найден с ид: " + moduleId);
        }

        // проверка дубликатов
        if (accountTemplateBindingRepository.existsByModuleIdAndAccountIdAndTemplateId(moduleId, accountId, templateId)) {
            throw new BindingAlreadyExistsException("Связка аккаунта и шаблона существует");
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

        // Если создаём активный биндинг — выключаем остальные
        if (active) {
            deactivateOtherBindings(moduleId, null);
        }

        AccountTemplateBinding b = AccountTemplateBinding.builder()
                .moduleId(moduleId)
                .accountId(accountId)
                .templateId(templateId)
                .promtId(promtId)
                .active(active)
                .userUuid(uuid)
                .build();

        AccountTemplateBinding saved = accountTemplateBindingRepository.save(b);
        return convertToBindingResponse(uuid, saved);
    }

    @Transactional
    public BindingResponse update(String uuid, Long id, Long moduleId, Long accountId, Long templateId, Long promtId, boolean active) {
        if (!orderModulesRepository.existsById(moduleId)) {
            throw new OrderModuleNotFoundException("Модуль не найден с ид: " + moduleId);
        }

        AccountTemplateBinding b = accountTemplateBindingRepository.findById(id)
                .orElseThrow(() -> new BindingNotFoundException("Связка аккаунта и шаблона не найдена: " + id));

        if (!uuid.equals(b.getUserUuid())) {
            throw new AccessDeniedException("Эта привязка не принадлежит текущему пользователю");
        }

        // Проверка дубликата (кроме текущего)
        boolean exists = accountTemplateBindingRepository.existsByModuleIdAndAccountIdAndTemplateId(moduleId, accountId, templateId);
        if (exists && !(b.getModuleId().equals(moduleId)
                && b.getAccountId().equals(accountId)
                && b.getTemplateId().equals(templateId))) {
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

        // Если обновляем и ставим active=true — выключаем остальные
        if (active) {
            deactivateOtherBindings(moduleId, id);
        }

        // Обновление
        b.setModuleId(moduleId);
        b.setAccountId(accountId);
        b.setTemplateId(templateId);
        b.setPromtId(promtId);
        b.setActive(active);

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

        deactivateOtherBindings(b.getModuleId(), bindingId);
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
}
