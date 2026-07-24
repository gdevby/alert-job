package by.gdev.alert.job.llm.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Базовый класс для всех JPA‑сущностей, содержащий общий первичный ключ и дату создания.
 * <p>
 * Используется как родительский класс для всех доменных моделей, чтобы обеспечить:
 * <ul>
 *     <li>единый автоинкрементный идентификатор ({@code id});</li>
 *     <li>автоматическое хранение даты создания записи ({@code createdAt});</li>
 *     <li>минимизацию дублирования кода в сущностях;</li>
 *     <li>единый стиль аудита данных.</li>
 * </ul>
 * <p>
 * Поле {@code createdAt} должно устанавливаться вручную в дочерних сущностях
 * (обычно через {@code @PrePersist}), чтобы избежать зависимости от Hibernate‑специфичных аннотаций.
 */
@MappedSuperclass
@Data
@EqualsAndHashCode
@ToString
public class BasicId {

    /**
     * Уникальный идентификатор сущности.
     * Генерируется базой данных с использованием стратегии IDENTITY.
     */
    @Access(AccessType.PROPERTY)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Дата и время создания записи.
     * Устанавливается один раз при сохранении сущности.
     * Не обновляется при изменениях.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Автоматически устанавливает createdAt при первом сохранении.
     */
    @PrePersist
    protected void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

}
