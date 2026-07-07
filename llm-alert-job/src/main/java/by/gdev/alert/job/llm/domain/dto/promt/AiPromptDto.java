package by.gdev.alert.job.llm.domain.dto.promt;

import by.gdev.alert.job.llm.domain.promt.AiPromptType;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Описание промта. Используется в списках и UI.")
public class AiPromptDto {

    /** ID промта в БД */
    @Schema(description = "ID промта в базе данных", example = "12")
    private Long id;

    /** Версия промта (инкрементируется при обновлении) */
    @Schema(description = "Версия промта", example = "3")
    private Integer version;

    /** Имя промта */
    @Schema(description = "Имя промта")
    private String name;

    /** Текст промта */
    @Schema(description = "Текст промта")
    private String text;

    /** Дата создания записи */
    @Schema(description = "Дата создания промта", example = "2026-06-19T08:44:32")
    private LocalDateTime createdAt;

    /** Дата последнего обновления */
    @Schema(description = "Дата последнего обновления промта", example = "2026-06-19T09:12:10")
    private LocalDateTime updatedAt;
}

