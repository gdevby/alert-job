package by.gdev.alert.job.llm.repository.promt;

import by.gdev.alert.job.llm.domain.promt.AiPrompt;
import by.gdev.alert.job.llm.domain.promt.AiPromptType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Репозиторий для работы с сущностями {@link AiPrompt}.
 * <p>
 * Предоставляет методы для поиска промтов по типу.
 * Каждый тип промта существует в единственном экземпляре.
 */
public interface AiPromptRepository extends JpaRepository<AiPrompt, Long> {

    /**
     * Ищет промт по его типу.
     *
     * @param type тип промта
     * @return найденный промт или пустой Optional
     */
    Optional<AiPrompt> findByType(AiPromptType type);

    Optional<AiPrompt> findByModuleId(Long moduleId);
}
