package by.gdev.alert.job.llm.domain.dto.promt;

import lombok.Data;

@Data
public class PromptRequest {
    private String name;
    private String text;
}