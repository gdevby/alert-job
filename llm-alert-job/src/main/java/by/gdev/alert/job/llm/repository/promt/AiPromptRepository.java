package by.gdev.alert.job.llm.repository.promt;


import by.gdev.alert.job.llm.domain.promt.AiPrompt;
import by.gdev.alert.job.llm.domain.promt.AiPromptType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiPromptRepository extends JpaRepository<AiPrompt, Long> {
    Optional<AiPrompt> findByType(AiPromptType type);
}
