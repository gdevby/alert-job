package by.gdev.alert.job.llm.repository;

import by.gdev.alert.job.llm.domain.LlmUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LlmUserRepository extends JpaRepository<LlmUser, Long> {
    Optional<LlmUser> findByUuid(String uuid);
}

