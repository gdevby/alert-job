package by.gdev.alert.job.llm.domain.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Информация о модуле, от имени которого отправлены заказы")
public class AiOrderModulesDTO {

    @Schema(
            description = "ID модуля",
            example = "42"
    )
    private Long id;

    @Schema(
            description = "Название модуля",
            example = "FreelanceParser"
    )
    private String name;
}
