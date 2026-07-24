package by.gdev.alert.job.core.model.binding.dto;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

@Data
public class BindingResponse {
    @Schema(description = "ID привязки", required = true, example = "1")
    private Long id;
    @Schema(description = "ID модуля", required = true, example = "1")
    private Long moduleId;
    @Schema(description = "Название модуля", required = true, example = "Модуль 1")
    private String moduleName;
    @Schema(description = "ID учётной записи", required = true, example = "3")
    private Long accountId;
    @Schema(description = "Название учётной записи", required = true, example = "Аккаунт Kwork")
    private String accountName;
    @Schema(description = "ID шаблона", required = true, example = "1")
    private Long templateId;
    @Schema(description = "Название шаблона", required = true, example = "Шаблон 1")
    private String templateName;
    @Schema(description = "ID промта", required = true, example = "7")
    private Long promtId;
    @Schema(description = "Название промта", required = true, example = "DEFAULT_PROMPT")
    private String promtName;
    @Schema(description = "Активна ли привязка", required = true, example = "true")
    private Boolean active;
    @Schema(description = "Дата создания", required = true, example = "2026-07-02T18:50:12.340107")
    private String createdAt;
}
