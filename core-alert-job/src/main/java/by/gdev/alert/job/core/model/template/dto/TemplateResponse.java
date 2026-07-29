package by.gdev.alert.job.core.model.template.dto;

import lombok.Data;

@Data
public class TemplateResponse {
    private String name;
    private Long id;
    private String htmlTemplate;
    private String createdAt;
}