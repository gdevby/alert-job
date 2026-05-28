package by.gdev.alert.job.llm.repository;

import by.gdev.alert.job.llm.domain.AiReplyTemplate;
import by.gdev.alert.job.llm.domain.LlmUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiReplyTemplateRepository extends JpaRepository<AiReplyTemplate, Long> {

    // Основной метод: найти шаблон по пользователю и модулю
    Optional<AiReplyTemplate> findByUserAndModuleId(LlmUser user, Long moduleId);

    // Найти все шаблоны пользователя
    List<AiReplyTemplate> findByUser(LlmUser user);

    // Проверка существования
    boolean existsByUserAndModuleId(LlmUser user, Long moduleId);
}
