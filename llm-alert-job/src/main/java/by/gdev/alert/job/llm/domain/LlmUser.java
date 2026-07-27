package by.gdev.alert.job.llm.domain;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity, представляющая пользователя LLM‑сервиса.
 * <p>
 * Используется для хранения минимальной информации о пользователе,
 * необходимой для работы модулей автоответов и шаблонов.
 * <p>
 * Основные особенности:
 * <ul>
 *     <li>Содержит только UUID — внешний идентификатор пользователя.</li>
 *     <li>Наследует {@link BasicId}, включая автоинкрементный ID и дату создания.</li>
 *     <li>Может быть связан с шаблонами ответов ({@link AiReplyTemplate}).</li>
 * </ul>
 */
@Entity
@Getter
@Setter
public class LlmUser extends BasicId {

    /**
     * Уникальный UUID пользователя, приходящий из CORE‑сервиса.
     * Используется как внешний идентификатор.
     */
    private String uuid;
}
