package by.gdev.alert.job.parser.service.category.update;

import by.gdev.alert.job.parser.domain.db.Category;
import by.gdev.alert.job.parser.domain.db.SiteSourceJob;
import by.gdev.alert.job.parser.domain.db.Subcategory;
import by.gdev.alert.job.parser.repository.CategoryRepository;
import by.gdev.alert.job.parser.repository.SiteSourceJobRepository;
import by.gdev.alert.job.parser.repository.SubCategoryRepository;
import by.gdev.alert.job.parser.service.category.cleanup.CategoriesCleanupComponent;
import by.gdev.alert.job.parser.service.category.update.dto.changes.CategoryDiffResult;
import by.gdev.alert.job.parser.service.category.update.dto.tree.CategoryDTO;
import by.gdev.alert.job.parser.service.category.update.dto.tree.SiteDTO;
import by.gdev.alert.job.parser.service.category.update.dto.tree.SubcategoryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryDiffApplyService {

    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final SiteSourceJobRepository siteSourceJobRepository;
    private final CategoriesCleanupComponent categoriesCleanupComponent;

    @Transactional
    public void applyDiff(SiteSourceJob job, CategoryDiffResult diff) {
        moveSubcategories(diff);
        Map<String, Long> createdCategories = addNewCategories(job, diff);
        addNewSubcategories(job, diff, createdCategories);
        deleteRemoved(job, diff);
    }

    @Transactional
    public void applyOrder(SiteSourceJob job, SiteDTO parsedTree) {
        List<Category> categories =
                categoryRepository.findAllWithSubcategoriesBySourceId(job.getId());
        Map<String, Category> catByName = new HashMap<>();
        for (Category c : categories) {
            String name = c.getNativeLocName();
            if (name != null) {
                catByName.putIfAbsent(name, c);
            }
        }
        int catOrder = 1;
        for (CategoryDTO parsedCat : parsedTree.getCategories()) {
            Category cat = catByName.get(parsedCat.getName());
            if (cat == null) continue;
            cat.setOrder(catOrder++);
            Map<String, Subcategory> subByName = new HashMap<>();
            if (cat.getSubCategories() != null) {
                for (Subcategory s : cat.getSubCategories()) {
                    if (s.getNativeLocName() != null) {
                        subByName.putIfAbsent(s.getNativeLocName(), s);
                    }
                }
            }
            int subOrder = 1;
            for (SubcategoryDTO parsedSub : parsedCat.getSubcategories()) {
                Subcategory sub = subByName.get(parsedSub.getName());
                if (sub == null) continue;

                sub.setOrder(subOrder++);
            }
        }
        categoryRepository.saveAll(categories);
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
        SiteSourceJob managedJob = siteSourceJobRepository.findById(job.getId()).orElseThrow();
        for (CategoryDTO dto : diff.getNewCategories()) {
            Category c = new Category();
            c.setName(dto.getName());
            c.setNativeLocName(dto.getName());
            c.setLink(null);
            c.setParse(true);
            c.setSiteSourceJob(managedJob); // ← теперь OK
            categoryRepository.save(c);
            created.put(dto.getName(), c.getId());
        }
        return created;
    }


    private void addNewSubcategories(
            SiteSourceJob job,
            CategoryDiffResult diff,
            Map<String, Long> createdCategories){
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
        var removedSubIds = diff.getRemovedSubcategories().stream()
                .map(dto -> dto.getSubcategory().getId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (CategoryDiffResult.SubcategoryMoveDTO dto : diff.getMovedSubcategories()) {
            Long subId = dto.getSubcategory().getId();
            if (subId == null) continue;
            if (removedSubIds.contains(subId)) continue;
            Subcategory sc = subCategoryRepository.findById(subId).orElse(null);
            if (sc == null) continue;
            Long newParentId = dto.getNewParentId();

            if (newParentId == null) {
                String newParentName = dto.getNewParentName();
                Long siteId = sc.getCategory().getSiteSourceJob().getId();
                // Ищем существующую категорию по nativeLocName
                Category existing = categoryRepository
                        .findByNativeLocNameAndSiteSourceJobId(newParentName, siteId);

                if (existing != null) {
                    newParentId = existing.getId();
                } else {
                    Category newParent = new Category();
                    newParent.setName(null); // как в твоём дереве
                    newParent.setNativeLocName(newParentName);
                    newParent.setLink(null);
                    newParent.setParse(true);
                    newParent.setSiteSourceJob(sc.getCategory().getSiteSourceJob());
                    categoryRepository.save(newParent);
                    newParentId = newParent.getId();
                }
            }
            Category newParent = categoryRepository.findById(newParentId).orElse(null);
            if (newParent == null) continue;
            sc.setCategory(newParent);
            subCategoryRepository.save(sc);
        }
    }



}
