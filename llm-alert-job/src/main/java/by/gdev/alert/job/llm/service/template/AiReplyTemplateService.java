package by.gdev.alert.job.llm.service.template;

import by.gdev.alert.job.llm.domain.AiReplyTemplate;
import by.gdev.alert.job.llm.domain.LlmUser;
import by.gdev.alert.job.llm.domain.dto.template.CreateTemplateRequest;
import by.gdev.alert.job.llm.repository.AiReplyTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    /**
     * Репозиторий для работы с шаблонами.
     */
    private final AiReplyTemplateRepository templateRepository;

    /**
     * Сервис для управления пользователями LLM‑модуля.
     */
    private final LlmUserService userService;

    /**
     * Возвращает шаблон пользователя для указанного модуля.
     * Если пользователь отсутствует — создаётся автоматически.
     *
     * @param userUuid UUID пользователя
     * @param moduleId ID модуля
     * @return найденный шаблон или null
     */
    public AiReplyTemplate getTemplateForUserAndModule(String userUuid, Long moduleId) {
        LlmUser user = userService.getOrCreateUser(userUuid);
        return templateRepository.findByUserAndModuleId(user, moduleId).orElse(null);
    }

    /**
     * Возвращает список всех шаблонов пользователя.
     *
     * @param userUuid UUID пользователя
     * @return список шаблонов
     */
    @Transactional(readOnly = true)
    public List<AiReplyTemplate> getTemplatesByUser(String userUuid) {
        LlmUser user = userService.getOrCreateUser(userUuid);
        return templateRepository.findByUser(user);
    }

    /**
     * Создаёт новый шаблон или обновляет существующий.
     * <p>
     * Логика:
     * <ul>
     *     <li>проверяет корректность входных данных;</li>
     *     <li>гарантирует существование пользователя;</li>
     *     <li>ищет шаблон по пользователю и модулю;</li>
     *     <li>если найден — обновляет;</li>
     *     <li>если нет — создаёт новый.</li>
     * </ul>
     *
     * @param req DTO с данными шаблона
     * @return сохранённый шаблон
     */
    @Transactional
    public AiReplyTemplate createOrUpdateTemplate(CreateTemplateRequest req) {

        if (req.getUserUuid() == null || req.getModuleId() == null) {
            throw new IllegalArgumentException("userUuid and moduleId are required");
        }

        LlmUser user = userService.getOrCreateUser(req.getUserUuid());

        AiReplyTemplate template = templateRepository
                .findByUserAndModuleId(user, req.getModuleId())
                .orElseGet(AiReplyTemplate::new);

        template.setName(req.getName());
        template.setUser(user);
        template.setModuleId(req.getModuleId());
        template.setHtmlTemplate(req.getHtmlTemplate());

        return templateRepository.save(template);
    }

    /**
     * Возвращает шаблон по ID.
     *
     * @param templateId ID шаблона
     * @return найденный шаблон
     * @throws IllegalArgumentException если шаблон отсутствует
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
     *
     * @param id ID шаблона
     * @return true — если шаблон существует
     */
    public boolean exists(Long id) {
        return templateRepository.existsById(id);
    }
}
