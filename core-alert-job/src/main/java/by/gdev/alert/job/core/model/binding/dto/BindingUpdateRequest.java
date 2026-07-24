package by.gdev.alert.job.core.model.binding.dto;

import by.gdev.common.model.NotificationTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class BindingUpdateRequest {

    @Schema(description = "ID модуля", required = true, example = "1")
    private Long moduleId;

    @Schema(description = "ID учётной записи (аккаунта)", required = true, example = "3")
    private Long accountId;

    @Schema(description = "ID шаблона", required = true, example = "1")
    private Long templateId;

    @Schema(description = "ID промта", required = true, example = "7")
    private Long promtId;

    @Schema(description = "Активна ли привязка", required = true, example = "true")
    private Boolean active;

    @Schema(description = "Тип уведомления", required = true, example = "NONE")
    private NotificationTypeEnum notificationType;
}