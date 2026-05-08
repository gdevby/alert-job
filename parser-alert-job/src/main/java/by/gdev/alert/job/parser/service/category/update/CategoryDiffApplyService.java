package by.gdev.alert.job.parser.service.category.update;

import by.gdev.alert.job.parser.domain.db.Category;
import by.gdev.alert.job.parser.domain.db.SiteSourceJob;
import by.gdev.alert.job.parser.domain.db.Subcategory;
import by.gdev.alert.job.parser.repository.CategoryRepository;
import by.gdev.alert.job.parser.repository.SubCategoryRepository;
import by.gdev.alert.job.parser.service.category.cleanup.CategoriesCleanupComponent;
import by.gdev.alert.job.parser.service.category.update.dto.changes.CategoryDiffResult;
import by.gdev.alert.job.parser.service.category.update.dto.tree.CategoryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CategoryDiffApplyService {

    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final CategoriesCleanupComponent categoriesCleanupComponent;

    @Transactional
    public void applyDiff(SiteSourceJob job, CategoryDiffResult diff) {

        // Удаляем старые категории
        deleteRemoved(job, diff);

        // Создаём новые категории и сохраняем их ID
        Map<String, Long> createdCategories = addNewCategories(job, diff);

        // Создаём новые подкатегории (включая те, у которых parentId = null)
        addNewSubcategories(job, diff, createdCategories);

        // Перемещаем подкатегории
        moveSubcategories(diff);
    }

    private void deleteRemoved(SiteSourceJob job, CategoryDiffResult diff) {

        List<Long> removedCategoryIds = diff.getRemovedCategories().stream()
                .map(CategoryDTO::getId)
                .toList();

        List<Long> removedSubcategoryIds = diff.getRemovedSubcategories().stream()
                .map(s -> s.getSubcategory().getId())
                .toList();

        if (!removedCategoryIds.isEmpty() || !removedSubcategoryIds.isEmpty()) {
            categoriesCleanupComponent.deleteParserCategories(
                    removedCategoryIds,
                    removedSubcategoryIds
            );
        }
    }

    private Map<String, Long> addNewCategories(SiteSourceJob job, CategoryDiffResult diff) {

        Map<String, Long> created = new HashMap<>();

        for (CategoryDTO dto : diff.getNewCategories()) {
            Category c = new Category();
            c.setName(dto.getName());
            c.setNativeLocName(dto.getName());
            c.setLink(null);
            c.setParse(true);
            c.setSiteSourceJob(job);
            categoryRepository.save(c);

            created.put(dto.getName(), c.getId());
        }

        return created;
    }

    private void addNewSubcategories(
            SiteSourceJob job,
            CategoryDiffResult diff,
            Map<String, Long> createdCategories
    ) {

        for (CategoryDiffResult.SubcategoryWithParentDTO dto : diff.getNewSubcategories()) {

            Long parentId = dto.getParentId();

            // Если категория новая → parentId = null → ищем по имени
            if (parentId == null) {
                parentId = createdCategories.get(dto.getParentName());
            }

            Category parent = categoryRepository.findById(parentId)
                    .orElseThrow(() -> new IllegalStateException("Parent category not found"));

            Subcategory sc = new Subcategory();
            sc.setName(dto.getSubcategory().getName());
            sc.setNativeLocName(dto.getSubcategory().getName());
            sc.setLink(null);
            sc.setParse(true);
            sc.setCategory(parent);

            subCategoryRepository.save(sc);
        }
    }

    private void moveSubcategories(CategoryDiffResult diff) {

        for (CategoryDiffResult.SubcategoryMoveDTO dto : diff.getMovedSubcategories()) {

            Subcategory sc = subCategoryRepository.findById(dto.getSubcategory().getId())
                    .orElseThrow(() -> new IllegalStateException("Subcategory not found"));

            Category newParent = categoryRepository.findById(dto.getNewParentId())
                    .orElseThrow(() -> new IllegalStateException("New parent not found"));

            sc.setCategory(newParent);
            subCategoryRepository.save(sc);
        }
    }
}


