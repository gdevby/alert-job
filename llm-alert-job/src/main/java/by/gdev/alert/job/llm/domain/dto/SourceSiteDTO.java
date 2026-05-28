package by.gdev.alert.job.llm.domain.dto;

import lombok.Data;

@Data
public class SourceSiteDTO {
    private Long id;
    private Long source;
    private String sourceName;
    private Long category;
    private String categoryName;
    private Long subCategory;
    private String subCategoryName;
}
