package by.gdev.alert.job.llm.domain.promt;

import by.gdev.alert.job.llm.domain.BasicId;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity, представляющая AI‑промт, используемый системой автоответов.
 * <p>
 * Хранит текст промта, его тип, имя, версию и временные метки.
 * Промт может обновляться — при этом версия автоматически увеличивается.
 * <p>
 * Основные особенности:
 * <ul>
 *     <li>Содержит длинный текст промта (LONGTEXT).</li>
 *     <li>Версия инкрементируется при обновлении.</li>
 *     <li>Автоматически выставляет createdAt и updatedAt при создании.</li>
 *     <li>При обновлении обновляет только updatedAt.</li>
 * </ul>
 */
@Entity
@Table(name = "ai_prompt")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiPrompt extends BasicId {

    /**
     * Тип промта (категория), определяет назначение и область применения.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private AiPromptType type;

    /**
     * Человекочитаемое имя промта.
     * Может использоваться в UI и для поиска.
     */
    @Column(name = "name", length = 512)
    private String name;

    /**
     * Полный текст промта, который используется LLM.
     * Хранится в формате LONGTEXT.
     */
    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String promptText;

    /**
     * Версия промта.
     * Инкрементируется при каждом обновлении.
     */
    @Column(nullable = false)
    private Integer version;

    /**
     * Дата и время создания записи.
     * Устанавливается автоматически в момент сохранения.
     */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * Дата и время последнего обновления записи.
     * Обновляется при каждом изменении.
     */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Устанавливает createdAt и updatedAt при первом сохранении.
     * Если версия не указана — устанавливает 1.
     */
    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;

        if (version == null) version = 1;
    }

    /**
     * Обновляет updatedAt при каждом изменении сущности.
     */
    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
