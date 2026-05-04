package by.gdev.alert.job.core.model.category;

import by.gdev.alert.job.core.model.category.tree.CategoryDTO;
import lombok.Data;

import java.util.List;

@Data
public class CategoryDiffDTO {

    private List<CategoryDTO> newCategories;
    private List<CategoryDTO> removedCategories;
    private List<CategoryDiffResult.CategoryMoveDTO> movedCategories;

    private List<CategoryDiffResult.SubcategoryWithParentDTO> newSubcategories;
    private List<CategoryDiffResult.SubcategoryWithParentDTO> removedSubcategories;
    private List<CategoryDiffResult.SubcategoryMoveDTO> movedSubcategories;
}

