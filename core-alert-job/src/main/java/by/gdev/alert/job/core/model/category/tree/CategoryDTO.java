package by.gdev.alert.job.core.model.category.tree;

import lombok.Data;

import java.util.List;

@Data
public class CategoryDTO {
    private Long id;
    private String name;
    private List<SubcategoryDTO> subcategories;
}
