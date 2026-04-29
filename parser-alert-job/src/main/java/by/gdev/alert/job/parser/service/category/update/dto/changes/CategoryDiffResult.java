package by.gdev.alert.job.parser.service.category.update.dto.changes;

import by.gdev.alert.job.parser.service.category.update.dto.tree.CategoryDTO;
import by.gdev.alert.job.parser.service.category.update.dto.tree.SubcategoryDTO;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CategoryDiffResult {

    // --- Категории ---
    private final List<CategoryDTO> newCategories = new ArrayList<>();
    private final List<CategoryDTO> removedCategories = new ArrayList<>();
    private final List<CategoryMoveDTO> movedCategories = new ArrayList<>();

    // --- Подкатегории ---
    private final List<SubcategoryWithParentDTO> newSubcategories = new ArrayList<>();
    private final List<SubcategoryWithParentDTO> removedSubcategories = new ArrayList<>();
    private final List<SubcategoryMoveDTO> movedSubcategories = new ArrayList<>();

    public boolean isEmpty() {
        return newCategories.isEmpty()
                && removedCategories.isEmpty()
                && movedCategories.isEmpty()
                && newSubcategories.isEmpty()
                && removedSubcategories.isEmpty()
                && movedSubcategories.isEmpty();
    }

    // --- DTO для перемещённых категорий ---
    @Data
    public static class CategoryMoveDTO {
        private final CategoryDTO oldCategory;
        private final CategoryDTO newCategory;
    }

    // --- DTO для новых/удалённых подкатегорий ---
    @Data
    public static class SubcategoryWithParentDTO {
        private final Long parentId;
        private final String parentName;
        private final SubcategoryDTO subcategory;
    }

    // --- DTO для перемещённых подкатегорий ---
    @Data
    public static class SubcategoryMoveDTO {
        private final Long oldParentId;
        private final String oldParentName;
        private final Long newParentId;
        private final String newParentName;
        private final SubcategoryDTO subcategory;
    }
}
