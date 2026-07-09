package by.gdev.alert.job.llm.repository.promt;

import by.gdev.alert.job.llm.domain.LlmUser;
import by.gdev.alert.job.llm.domain.promt.AiPrompt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с сущностями {@link AiPrompt}.
 * <p>
 * Предоставляет методы для поиска промтов по типу.
 * Каждый тип промта существует в единственном экземпляре.
 */
public interface AiPromptRepository extends JpaRepository<AiPrompt, Long> {

    List<AiPrompt> findByUserAndName(LlmUser user, String name);

    List<AiPrompt> findByUser(LlmUser user);

    Optional<AiPrompt> findByName(String name);

}
