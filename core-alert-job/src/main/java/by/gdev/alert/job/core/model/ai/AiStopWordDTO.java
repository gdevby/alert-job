package by.gdev.alert.job.core.model.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiStopWordDTO {
    private Long id;
    private String word;
    private String createdAt;
    private String updatedAt;
}