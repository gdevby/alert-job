package by.gdev.alert.job.llm.domain.promt;

import by.gdev.alert.job.llm.domain.BasicId;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_prompt")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiPrompt extends BasicId {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private AiPromptType type;

    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String promptText;

    @Column(nullable = false)
    private Integer version;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;

        if (version == null) version = 1;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

