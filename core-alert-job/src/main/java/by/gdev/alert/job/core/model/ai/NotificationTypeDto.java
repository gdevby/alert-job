package by.gdev.alert.job.core.model.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "DTO типа уведомления")
public class NotificationTypeDto {

    @Schema(description = "Код типа уведомления", example = "EMAIL", required = true)
    private String code;

    @Schema(description = "Описание типа уведомления", example = "Только Email", required = true)
    private String description;
}