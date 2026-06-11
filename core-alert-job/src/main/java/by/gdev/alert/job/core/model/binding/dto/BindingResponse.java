package by.gdev.alert.job.core.model.binding.dto;

import lombok.Data;

@Data
public class BindingResponse {
    private Long id;
    private String accountName;
    private String templateName;
    private String createdAt;
    private boolean active;
}
