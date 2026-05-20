package by.gdev.alert.job.parser.service.category.update.dto.changes;

import by.gdev.alert.job.parser.service.category.update.dto.tree.CategoryDTO;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class CategoryDiffDTO {

    private List<CategoryDTO> newCategories;
    private List<CategoryDTO> removedCategories;
    private List<CategoryDiffResult.CategoryMoveDTO> movedCategories;

    private List<CategoryDiffResult.SubcategoryWithParentDTO> newSubcategories;
    private List<CategoryDiffResult.SubcategoryWithParentDTO> removedSubcategories;
    private List<CategoryDiffResult.SubcategoryMoveDTO> movedSubcategories;
}



