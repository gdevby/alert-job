package by.gdev.alert.job.llm.domain.dto.promt;

import by.gdev.alert.job.llm.domain.promt.AiPromptType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO для краткого описания промта:
 *  - содержит служебную информацию (id, тип, версия, даты);
 *  - не включает текст промта (для экономии трафика и безопасности);
 *  - используется в списках и таблицах UI.
 */
@Data
@Builder
public class AiPromptDto {
    /** ID промта в БД */
    private Long id;

    /** Тип промта (категория) */
    private AiPromptType type;

    /** Версия промта (инкрементируется при обновлении) */
    private Integer version;

    /** Имя промта*/
    private String name;

    /** Дата создания записи */
    private LocalDateTime createdAt;

    /** Дата последнего обновления */
    private LocalDateTime updatedAt;
}
