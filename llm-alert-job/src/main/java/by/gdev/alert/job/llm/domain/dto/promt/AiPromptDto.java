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

    @Schema(description = "ID промта в базе данных", required = true, example = "12")
    private Long id;

    @Schema(description = "Версия промта (инкрементируется при обновлении)", required = true, example = "3")
    private Integer version;

    @Schema(description = "Имя промта", required = true, example = "Мой промт")
    private String name;

    @Schema(description = "Текст промта", required = true, example = "Ты — экспертный аналитик...")
    private String text;

    @Schema(description = "Дата создания промта", required = true, example = "2026-06-19T08:44:32")
    private LocalDateTime createdAt;

    @Schema(description = "Дата последнего обновления промта", required = true, example = "2026-06-19T09:12:10")
    private LocalDateTime updatedAt;
}