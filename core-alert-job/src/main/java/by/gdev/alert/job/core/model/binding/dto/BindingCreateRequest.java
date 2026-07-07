package by.gdev.alert.job.core.model.binding.dto;

import lombok.Data;

@Data
public class BindingCreateRequest {
    private Long moduleId;
    private Long accountId;
    private Long templateId;
    private Long promtId;
    private Boolean active = true; // по умолчанию true
}