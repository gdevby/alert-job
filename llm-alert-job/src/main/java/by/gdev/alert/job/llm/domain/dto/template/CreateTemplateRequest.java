package by.gdev.alert.job.llm.domain.dto.template;

import lombok.Data;

@Data
public class CreateTemplateRequest {
    private String userUuid;
    private Long moduleId;
    private String htmlTemplate;
}

