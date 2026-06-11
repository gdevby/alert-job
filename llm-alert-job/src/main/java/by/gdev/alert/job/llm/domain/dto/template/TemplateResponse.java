package by.gdev.alert.job.llm.domain.dto.template;

import lombok.Data;

@Data
public class TemplateResponse {
    private String name;
    private Long id;
    private String htmlTemplate;
    private String createdAt;
}
