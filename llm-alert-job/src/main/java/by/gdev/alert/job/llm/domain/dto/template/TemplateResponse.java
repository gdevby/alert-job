package by.gdev.alert.job.llm.domain.dto.template;

import lombok.Data;

@Data
public class TemplateResponse {
    private Long id;
    private Long moduleId;
    private String htmlTemplate;
}
