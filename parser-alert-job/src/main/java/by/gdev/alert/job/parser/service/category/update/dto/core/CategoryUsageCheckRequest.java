package by.gdev.alert.job.parser.service.category.update.dto.core;

import lombok.Data;

@Data
public class CategoryUsageCheckRequest {
    private Long siteId;
    private Long categoryId;
    private Long subcategoryId;
}
