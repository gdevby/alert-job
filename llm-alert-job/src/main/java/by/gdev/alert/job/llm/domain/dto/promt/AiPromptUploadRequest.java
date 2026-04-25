package by.gdev.alert.job.llm.domain.dto.promt;

import by.gdev.alert.job.llm.domain.promt.AiPromptType;
import lombok.Data;

@Data
public class AiPromptUploadRequest {
    private AiPromptType type;
}

