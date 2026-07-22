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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryDiffApplyService {

    private static final Pattern CATEGORY_ID_IN_LINK = Pattern.compile("[?&]category=(\\d+)");
    private static final Pattern SUBCATEGORY_ID_IN_LINK = Pattern.compile("[?&]subcategory=(\\d+)");

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
        List<Category> categories = categoryRepository.findAllWithSubcategoriesBySourceId(job.getId());
        Map<String, Category> catByName = buildCategoryLookup(categories);
        Map<Long, Category> catBySourceId = buildCategoryLookupBySourceId(categories);

        List<Subcategory> allSubcategories = new ArrayList<>();
        Set<Long> updatedCategoryIds = new HashSet<>();
        Set<Long> updatedSubcategoryIds = new HashSet<>();

        int fallbackCatOrder = 1;
        for (CategoryDTO parsedCat : parsedTree.getCategories()) {
            Category cat = findCategory(catByName, catBySourceId, parsedCat);
            if (cat == null) {
                continue;
            }
            syncCategoryName(cat, parsedCat.getName());

            Integer order = parsedCat.getOrder() != null ? parsedCat.getOrder() : fallbackCatOrder;
            cat.setOrder(order);
            fallbackCatOrder = Math.max(fallbackCatOrder, order) + 1;
            updatedCategoryIds.add(cat.getId());

            Map<String, Subcategory> subByName = buildSubcategoryLookup(cat.getSubCategories());
            Map<Long, Subcategory> subBySourceId = buildSubcategoryLookupBySourceId(cat.getSubCategories());

            int fallbackSubOrder = 1;
            for (SubcategoryDTO parsedSub : parsedCat.getSubcategories()) {
                Subcategory sub = findSubcategory(subByName, subBySourceId, parsedSub);
                if (sub == null) {
                    continue;
                }
                syncSubcategoryName(sub, parsedSub.getName());

                Integer subOrder = parsedSub.getOrder() != null ? parsedSub.getOrder() : fallbackSubOrder;
                sub.setOrder(subOrder);
                fallbackSubOrder = Math.max(fallbackSubOrder, subOrder) + 1;
                allSubcategories.add(sub);
                updatedSubcategoryIds.add(sub.getId());
            }
        }

        int trailingCatOrder = fallbackCatOrder;
        for (Category cat : categories) {
            if (!updatedCategoryIds.contains(cat.getId())) {
                cat.setOrder(trailingCatOrder++);
            }
            if (cat.getSubCategories() != null) {
                int trailingSubOrder = cat.getSubCategories().stream()
                        .map(Subcategory::getOrder)
                        .filter(Objects::nonNull)
                        .max(Integer::compareTo)
                        .map(max -> max + 1)
                        .orElse(1);
                for (Subcategory sub : cat.getSubCategories()) {
                    if (!updatedSubcategoryIds.contains(sub.getId())) {
                        sub.setOrder(trailingSubOrder++);
                        allSubcategories.add(sub);
                    }
                }
            }
        }

        categoryRepository.saveAll(categories);
        if (!allSubcategories.isEmpty()) {
            subCategoryRepository.saveAll(allSubcategories);
        }
    }

    private void syncCategoryName(Category cat, String parsedName) {
        if (parsedName == null || parsedName.isBlank()) {
            return;
        }
        cat.setNativeLocName(parsedName);
        cat.setName(parsedName);
    }

    private void syncSubcategoryName(Subcategory sub, String parsedName) {
        if (parsedName == null || parsedName.isBlank()) {
            return;
        }
        sub.setNativeLocName(parsedName);
        sub.setName(parsedName);
    }

    private Map<String, Category> buildCategoryLookup(List<Category> categories) {
        Map<String, Category> lookup = new HashMap<>();
        for (Category category : categories) {
            registerName(lookup, category.getNativeLocName(), category);
            registerName(lookup, category.getName(), category);
        }
        return lookup;
    }

    private Map<String, Subcategory> buildSubcategoryLookup(Set<Subcategory> subcategories) {
        Map<String, Subcategory> lookup = new HashMap<>();
        if (subcategories == null) {
            return lookup;
        }
        for (Subcategory subcategory : subcategories) {
            registerName(lookup, subcategory.getNativeLocName(), subcategory);
            registerName(lookup, subcategory.getName(), subcategory);
        }
        return lookup;
    }

    private <T> void registerName(Map<String, T> lookup, String name, T value) {
        if (name != null && !name.isBlank()) {
            lookup.putIfAbsent(name, value);
        }
    }

    private Map<Long, Category> buildCategoryLookupBySourceId(List<Category> categories) {
        Map<Long, Category> lookup = new HashMap<>();
        for (Category category : categories) {
            Long sourceId = extractCategoryIdFromLink(category.getLink());
            if (sourceId != null) {
                lookup.putIfAbsent(sourceId, category);
            }
        }
        return lookup;
    }

    private Map<Long, Subcategory> buildSubcategoryLookupBySourceId(Set<Subcategory> subcategories) {
        Map<Long, Subcategory> lookup = new HashMap<>();
        if (subcategories == null) {
            return lookup;
        }
        for (Subcategory subcategory : subcategories) {
            Long sourceId = extractSubcategoryIdFromLink(subcategory.getLink());
            if (sourceId != null) {
                lookup.putIfAbsent(sourceId, subcategory);
            }
        }
        return lookup;
    }

    private Category findCategory(
            Map<String, Category> byName,
            Map<Long, Category> bySourceId,
            CategoryDTO parsedCat) {
        Category cat = findByName(byName, parsedCat.getName());
        if (cat != null) {
            return cat;
        }
        if (parsedCat.getSourceId() != null) {
            cat = bySourceId.get(parsedCat.getSourceId());
            if (cat != null) {
                return cat;
            }
        }
        if (parsedCat.getLink() != null) {
            Long sourceId = extractCategoryIdFromLink(parsedCat.getLink());
            if (sourceId != null) {
                return bySourceId.get(sourceId);
            }
        }
        return null;
    }

    private Subcategory findSubcategory(
            Map<String, Subcategory> byName,
            Map<Long, Subcategory> bySourceId,
            SubcategoryDTO parsedSub) {
        Subcategory sub = findSubcategoryByName(byName, parsedSub.getName());
        if (sub != null) {
            return sub;
        }
        if (parsedSub.getSourceId() != null) {
            sub = bySourceId.get(parsedSub.getSourceId());
            if (sub != null) {
                return sub;
            }
        }
        if (parsedSub.getLink() != null) {
            Long sourceId = extractSubcategoryIdFromLink(parsedSub.getLink());
            if (sourceId != null) {
                return bySourceId.get(sourceId);
            }
        }
        return null;
    }

    public static Long extractCategoryIdFromLink(String link) {
        return extractIdFromLink(link, CATEGORY_ID_IN_LINK);
    }

    public static Long extractSubcategoryIdFromLink(String link) {
        return extractIdFromLink(link, SUBCATEGORY_ID_IN_LINK);
    }

    private static Long extractIdFromLink(String link, Pattern pattern) {
        if (link == null || link.isBlank()) {
            return null;
        }
        Matcher matcher = pattern.matcher(link);
        if (matcher.find()) {
            return Long.parseLong(matcher.group(1));
        }
        return null;
    }

    private Category findByName(Map<String, Category> lookup, String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return lookup.get(name);
    }

    private Subcategory findSubcategoryByName(Map<String, Subcategory> lookup, String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return lookup.get(name);
    }

    private void deleteRemoved(SiteSourceJob job, CategoryDiffResult diff) {

        List<Long> removedCategoryIds = diff.getRemovedCategories()
                .stream()
                .map(CategoryDTO::getId)
                .toList();

        List<Long> removedSubcategoryIds = diff.getRemovedSubcategories()
                .stream()
                .map(dto -> dto.getSubcategory().getId())
                .toList();

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
            c.setLink(dto.getLink());
            c.setParse(true);
            c.setSiteSourceJob(managedJob);
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
            if (parentId == null) {
                parentId = createdCategories.get(dto.getParentName());
            }
            if (parentId == null || !categoryRepository.existsById(parentId)) {
                continue;
            }
            Category parent = categoryRepository.findById(parentId).orElse(null);
            if (parent == null) continue;

            Subcategory sc = new Subcategory();
            sc.setName(dto.getSubcategory().getName());
            sc.setNativeLocName(dto.getSubcategory().getName());
            sc.setLink(dto.getSubcategory().getLink());
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
                Category existing = categoryRepository
                        .findByNativeLocNameAndSiteSourceJobId(newParentName, siteId);

                if (existing != null) {
                    newParentId = existing.getId();
                } else {
                    Category newParent = new Category();
                    newParent.setName(null);
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
