package by.gdev.alert.job.parser.service.category.update.dto.tree;

import lombok.Data;

import java.util.List;

@Data
public class CategoryDTO {
    private Long id;
    private String name;
    private Integer order;
    private Long sourceId;
    private String link;
    private List<SubcategoryDTO> subcategories;
}
