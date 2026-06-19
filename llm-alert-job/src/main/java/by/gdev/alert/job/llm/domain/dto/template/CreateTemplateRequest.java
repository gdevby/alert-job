package by.gdev.alert.job.llm.domain.dto.template;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Запрос на создание шаблона")
public class CreateTemplateRequest {
    @Schema(description = "Название шаблона", example = "Order Template #1")
    private String name;
    @Schema(description = "UUID пользователя", example = "123e4567-e89b-12d3-a456-426614174000")
    private String userUuid;
    @Schema(description = "ID модуля", example = "42")
    private Long moduleId;
    @Schema(description = "HTML содержимое шаблона")
    private String htmlTemplate;
}

