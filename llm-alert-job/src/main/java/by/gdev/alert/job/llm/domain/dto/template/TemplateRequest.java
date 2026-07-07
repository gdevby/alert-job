package by.gdev.alert.job.llm.domain.dto.template;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Запрос на операции с шаблоном")
public class TemplateRequest {
    @Schema(description = "Название шаблона", example = "Order Template #1")
    private String name;
    @Schema(description = "Cодержимое шаблона")
    private String text;
}

