package by.gdev.alert.job.llm.service.template;

import by.gdev.alert.job.llm.domain.AiReplyTemplate;
import by.gdev.alert.job.llm.domain.LlmUser;
import by.gdev.alert.job.llm.domain.dto.template.TemplateRequest;
import by.gdev.alert.job.llm.repository.AiReplyTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Сервис для управления HTML‑шаблонами автоответов.
 * <p>
 * Отвечает за:
 * <ul>
 *     <li>создание и обновление пользовательских шаблонов;</li>
 *     <li>получение шаблонов по пользователю и модулю;</li>
 *     <li>получение шаблона по ID;</li>
 *     <li>проверку существования шаблона;</li>
 *     <li>автоматическое создание записи пользователя при необходимости.</li>
 * </ul>
 * <p>
 * Каждый пользователь может иметь один шаблон на модуль.
 */
@Service
@RequiredArgsConstructor
public class AiReplyTemplateService {

    private final AiReplyTemplateRepository templateRepository;
    private final LlmUserService userService;

    /**
     * Возвращает список всех шаблонов пользователя.
     */
    @Transactional(readOnly = true)
    public List<AiReplyTemplate> getTemplatesByUser(String userUuid) {
        LlmUser user = userService.getOrCreateUser(userUuid);
        List<AiReplyTemplate> templates = templateRepository.findByUser(user);

        try {
            AiReplyTemplate defaultTemplate = getDefaultTemplate();
            boolean alreadyExists = templates.stream()
                    .anyMatch(t -> t.getId().equals(defaultTemplate.getId()));
            if (!alreadyExists) {
                templates.add(defaultTemplate);
            }
        } catch (IllegalStateException e) {
            // Дефолтный шаблон не найден, просто возвращаем пользовательские шаблоны
        }
        return templates;
    }

    /**
     * Создаёт новый шаблон.
     */
    @Transactional
    public AiReplyTemplate createOrUpdateTemplate(String uuid, TemplateRequest req) {
        if (uuid == null) {
            throw new IllegalArgumentException("UUID-user-header обязателен");
        }

        LlmUser user = userService.getOrCreateUser(uuid);

        // Ищем шаблон с таким именем у пользователя
        Optional<AiReplyTemplate> existing = templateRepository.findByUserAndName(user, req.getName());

        if (existing.isPresent()) {
            // Обновляем существующий
            AiReplyTemplate template = existing.get();
            template.setText(req.getText()); // обновляем содержимое
            return templateRepository.save(template);
        } else {
            // Создаём новый
            AiReplyTemplate template = new AiReplyTemplate();
            template.setName(req.getName());
            template.setUser(user);
            template.setText(req.getText());
            return templateRepository.save(template);
        }
    }

    @Transactional
    public void deleteTemplate(String uuid, Long templateId) {
        // Находим шаблон
        AiReplyTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Шаблон не найден для id: " + templateId));
        // Запрещаем удаление DEFAULT_TEMPLATE
        if ("DEFAULT_TEMPLATE".equals(template.getName())) {
            throw new IllegalArgumentException("Нельзя удалить системный шаблон по умолчанию");
        }
        // Проверяем принадлежность пользователю
        LlmUser user = userService.getOrCreateUser(uuid);
        if (template.getUser() == null || !template.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("У вас нет прав на удаление этого шаблона");
        }
        // Удаляем
        templateRepository.delete(template);
    }

    /**
     * Возвращает шаблон по ID.
     */
    @Transactional(readOnly = true)
    public AiReplyTemplate getTemplateById(String uuid, Long templateId) {
        AiReplyTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Шаблон не найден для id: " + templateId));

        // Если шаблон глобальный (не привязан к пользователю) — доступ разрешён
        if (template.getUser() == null) {
            return template;
        }

        // Проверяем, что шаблон принадлежит текущему пользователю
        if (!template.getUser().getUuid().equals(uuid)) {
            throw new IllegalArgumentException("Шаблон не принадлежит пользователю");
        }

        return template;
    }

    /**
     * Возвращает шаблон по ID.
     */
    @Transactional(readOnly = true)
    public AiReplyTemplate getTemplateById(Long templateId) {
        return templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Шаблон не найден для id: " + templateId
                ));
    }

    /**
     * Проверяет существование шаблона по ID.
     */
    public boolean exists(String uuid, Long id) {
        AiReplyTemplate template = templateRepository.findById(id).orElse(null);
        if (template == null) return false;
        if (template.getUser() == null) return true; // глобальный доступен
        return template.getUser().getUuid().equals(uuid);
    }

    /**
     * Возвращает шаблон по умолчанию (DEFAULT_TEMPLATE).
     */
    @Transactional(readOnly = true)
    public AiReplyTemplate getDefaultTemplate() {
        List<AiReplyTemplate> defaults = templateRepository.findByName("DEFAULT_TEMPLATE");
        if (defaults.isEmpty()) {
            throw new IllegalStateException("Не найден шаблон по умолчанию DEFAULT_TEMPLATE");
        }
        return defaults.get(0);
    }

}

