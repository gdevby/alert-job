package by.gdev.alert.job.llm.repository;

import by.gdev.alert.job.llm.domain.AiReplyTemplate;
import by.gdev.alert.job.llm.domain.LlmUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с HTML‑шаблонами автоответов {@link AiReplyTemplate}.
 * <p>
 * Предоставляет методы для поиска шаблонов по пользователю и модулю,
 * а также получения всех шаблонов конкретного пользователя.
 */
public interface AiReplyTemplateRepository extends JpaRepository<AiReplyTemplate, Long> {

    /**
     * Ищет шаблон по пользователю и ID модуля.
     * Каждый пользователь может иметь один шаблон на модуль.
     *
     * @param user     пользователь LLM‑модуля
     * @param moduleId ID модуля
     * @return найденный шаблон или пустой Optional
     */
    Optional<AiReplyTemplate> findByUserAndModuleId(LlmUser user, Long moduleId);

    /**
     * Возвращает все шаблоны, созданные указанным пользователем.
     *
     * @param user пользователь LLM‑модуля
     * @return список шаблонов
     */
    List<AiReplyTemplate> findByUser(LlmUser user);
}
