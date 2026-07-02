package by.gdev.alert.job.llm.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity, представляющая HTML‑шаблон автоответа, используемый AI.
 * <p>
 * Шаблон хранится в базе и может быть привязан к конкретному пользователю
 * или использоваться как общий для модуля. Содержит HTML‑контент, который
 * подставляется в итоговый ответ при генерации уведомлений.
 * <p>
 * Основные особенности:
 * <ul>
 *     <li>Поддерживает хранение больших HTML‑фрагментов через {@code @Lob}.</li>
 *     <li>Может быть привязан к конкретному пользователю ({@link LlmUser}).</li>
 *     <li>Может быть связан с модулем через {@code moduleId}.</li>
 *     <li>Используется системой автоответов для формирования email/telegram сообщений.</li>
 * </ul>
 */
@Entity
@Getter
@Setter
public class AiReplyTemplate extends BasicId {

    /**
     * Название шаблона.
     * Используется в UI и для идентификации шаблонов.
     */
    @Column(name = "name", length = 512)
    private String name;

    /**
     * Пользователь, которому принадлежит шаблон.
     * Может быть {@code null}, если шаблон общий для модуля.
     */
    @ManyToOne
    private LlmUser user;

    /**
     * ID модуля, для которого предназначен шаблон.
     * Если {@code user == null}, шаблон считается модульным.
     */
    private Long moduleId;

    /**
     * HTML‑контент шаблона автоответа.
     * Хранится как LOB, так как может быть большим.
     */
    @Lob
    private String htmlTemplate;
}
