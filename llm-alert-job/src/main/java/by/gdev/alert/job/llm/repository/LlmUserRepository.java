package by.gdev.alert.job.llm.repository;

import by.gdev.alert.job.llm.domain.LlmUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Репозиторий для работы с сущностями {@link LlmUser}.
 * <p>
 * Предоставляет метод для поиска пользователя по его UUID.
 * Локальная таблица хранит только UUID, остальные данные находятся в CORE‑модуле.
 */
public interface LlmUserRepository extends JpaRepository<LlmUser, Long> {

    /**
     * Ищет пользователя по UUID.
     *
     * @param uuid уникальный идентификатор пользователя
     * @return найденный пользователь или пустой Optional
     */
    Optional<LlmUser> findByUuid(String uuid);
}
