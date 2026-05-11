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

import java.util.ArrayList;
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

        // 1. Удаляем старые категории и ВСЕ их подкатегории
        deleteRemoved(job, diff);

        // 2. Создаём новые категории
        Map<String, Long> createdCategories = addNewCategories(job, diff);

        // 3. Создаём новые подкатегории
        addNewSubcategories(job, diff, createdCategories);

        // 4. Перемещаем существующие подкатегории
        moveSubcategories(diff);
    }

    private void deleteRemoved(SiteSourceJob job, CategoryDiffResult diff) {

        List<Long> removedCategoryIds = diff.getRemovedCategories()
                .stream()
                .map(CategoryDTO::getId)
                .toList();

        // Удаляем ВСЕ подкатегории, которые diff пометил как удалённые
        List<Long> removedSubcategoryIds = diff.getRemovedSubcategories()
                .stream()
                .map(dto -> dto.getSubcategory().getId())
                .toList();

        // ПЛЮС подкатегории удалённых категорий
        if (!removedCategoryIds.isEmpty()) {
            removedSubcategoryIds = new ArrayList<>(removedSubcategoryIds);
            removedSubcategoryIds.addAll(
                    subCategoryRepository.findAllByCategoryIdIn(removedCategoryIds)
                            .stream()
                            .map(Subcategory::getId)
                            .toList()
            );
        }

        if (!removedCategoryIds.isEmpty() || !removedSubcategoryIds.isEmpty()) {
            categoriesCleanupComponent.deleteParserCategories(
                    removedCategoryIds,
                    removedSubcategoryIds,
                    job.getName()
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

            // Новая категория → parentId = null → ищем по имени
            if (parentId == null) {
                parentId = createdCategories.get(dto.getParentName());
            }

            if (parentId == null || !categoryRepository.existsById(parentId)) {
                // Родитель удалён или не создан — пропускаем
                continue;
            }

            Category parent = categoryRepository.findById(parentId).orElse(null);
            if (parent == null) continue;

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

            Long subId = dto.getSubcategory().getId();
            Long newParentId = dto.getNewParentId();

            // Пропускаем новые подкатегории (id = null)
            if (subId == null) continue;

            // Пропускаем перемещения в несуществующие категории
            if (newParentId == null || !categoryRepository.existsById(newParentId)) continue;

            // Пропускаем перемещения удалённых подкатегорий
            if (!subCategoryRepository.existsById(subId)) continue;

            Subcategory sc = subCategoryRepository.findById(subId).orElse(null);
            if (sc == null) continue;

            Category newParent = categoryRepository.findById(newParentId).orElse(null);
            if (newParent == null) continue;

            sc.setCategory(newParent);
            subCategoryRepository.save(sc);
        }
    }
}
