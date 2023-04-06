package by.gdev.common.model;

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
    private boolean openForAll;

}
