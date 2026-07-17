package by.gdev.alert.job.core.model.binding.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class BindingCreateRequest {

    @Schema(description = "ID модуля", required = true, example = "1")
    private Long moduleId;

    @Schema(description = "ID учётной записи (аккаунта)", required = true, example = "3")
    private Long accountId;

    @Schema(description = "ID шаблона", required = true, example = "1")
    private Long templateId;

    @Schema(description = "ID промта", required = true, example = "7")
    private Long promtId;

    @Schema(description = "Активна ли привязка", required = false, example = "true", defaultValue = "true")
    private Boolean active = true;
}