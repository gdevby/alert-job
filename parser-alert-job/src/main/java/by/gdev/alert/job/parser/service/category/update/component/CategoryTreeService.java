package by.gdev.alert.job.parser.service.category.update.component;

import by.gdev.alert.job.parser.domain.db.Category;
import by.gdev.alert.job.parser.domain.db.SiteSourceJob;
import by.gdev.alert.job.parser.domain.db.Subcategory;
import by.gdev.alert.job.parser.repository.CategoryRepository;
import by.gdev.alert.job.parser.service.category.ParsedCategory;
import by.gdev.alert.job.parser.service.category.update.dto.changes.CategoryDiffResult;
import by.gdev.alert.job.parser.service.category.update.dto.tree.CategoryDTO;
import by.gdev.alert.job.parser.service.category.update.dto.tree.SiteDTO;
import by.gdev.alert.job.parser.service.category.update.dto.tree.SubcategoryDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static by.gdev.common.model.CategoryLexemes.ALL_CATEGORIES;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryTreeService {

    private final CategoryRepository categoryRepository;

    public SiteDTO buildTree(SiteSourceJob job) {
        categoryRepository.fixBrokenSubcategoryPositions(job.getId());

        List<Category> categories =
                categoryRepository.findAllWithSubcategoriesBySourceId(job.getId());

        SiteDTO site = new SiteDTO();
        site.setId(job.getId());
        site.setName(job.getName());

        // Имя категории → CategoryDTO
        Map<String, CategoryDTO> categoryMap = new HashMap<>();

        for (Category c : categories) {

            String catName = c.getNativeLocName();

            CategoryDTO catDto = categoryMap.computeIfAbsent(catName, n -> {
                CategoryDTO dto = new CategoryDTO();
                dto.setId(c.getId());
                dto.setName(n);
                dto.setSubcategories(new ArrayList<>());
                return dto;
            });

            if (c.getSubCategories() != null) {
                for (Subcategory s : c.getSubCategories()) {

                    if (s == null) continue;
                    if (s.getId() == null) continue;
                    if (s.getNativeLocName() == null || s.getNativeLocName().isBlank()) continue;

                    String subName = s.getNativeLocName();

                    boolean exists = catDto.getSubcategories().stream()
                            .anyMatch(x -> x.getName().equals(subName));

                    if (!exists) {
                        SubcategoryDTO sd = new SubcategoryDTO();
                        sd.setId(s.getId());
                        sd.setName(subName);
                        catDto.getSubcategories().add(sd);
                    }
                }
            }
        }

        site.setCategories(new ArrayList<>(categoryMap.values()));
        return site;
    }


    public SiteDTO buildParsedTree(SiteSourceJob job, Map<ParsedCategory, List<ParsedCategory>> parsed) {
        SiteDTO site = new SiteDTO();
        site.setId(job.getId());
        site.setName(job.getName());

        // Имя категории → CategoryDTO
        Map<String, CategoryDTO> categoryMap = new HashMap<>();

        for (Map.Entry<ParsedCategory, List<ParsedCategory>> entry : parsed.entrySet()) {

            ParsedCategory parsedCat = entry.getKey();
            List<ParsedCategory> parsedSubs = entry.getValue();

            String catName = parsedCat.translatedName();

            CategoryDTO catDto = categoryMap.computeIfAbsent(catName, n -> {
                CategoryDTO dto = new CategoryDTO();
                dto.setName(n);
                dto.setSubcategories(new ArrayList<>());
                return dto;
            });

            for (ParsedCategory sub : parsedSubs) {
                String subName = sub.translatedName();
                if (subName == null || subName.isBlank()) continue;

                boolean exists = catDto.getSubcategories().stream()
                        .anyMatch(x -> x.getName().equals(subName));

                if (!exists) {
                    SubcategoryDTO sd = new SubcategoryDTO();
                    sd.setName(subName);
                    catDto.getSubcategories().add(sd);
                }
            }
        }

        site.setCategories(new ArrayList<>(categoryMap.values()));
        return site;
    }

    public CategoryDiffResult compareTrees(SiteDTO parsedTree, SiteDTO dbTree) {
        CategoryDiffResult result = new CategoryDiffResult();
        CategoryDiffResult added   = compareAdded(parsedTree, dbTree);
        CategoryDiffResult removed = compareRemoved(parsedTree, dbTree);
        CategoryDiffResult moved   = compareMoved(parsedTree, dbTree);

        result.getNewCategories().addAll(added.getNewCategories());
        result.getNewSubcategories().addAll(added.getNewSubcategories());
        result.getRemovedCategories().addAll(removed.getRemovedCategories());
        result.getRemovedSubcategories().addAll(removed.getRemovedSubcategories());
        result.getMovedSubcategories().addAll(moved.getMovedSubcategories());
        return result;
    }

    private CategoryDiffResult compareAdded(SiteDTO parsedTree, SiteDTO dbTree) {
        CategoryDiffResult diff = new CategoryDiffResult();
        compareCategories(parsedTree, dbTree, diff);
        //compareSubcategories(parsedTree, dbTree, diff);
        return diff;
    }

    private void compareCategories(SiteDTO parsedTree, SiteDTO dbTree, CategoryDiffResult diff) {
        // категории в БД по имени
        Set<String> dbNames = dbTree.getCategories().stream()
                .map(CategoryDTO::getName)
                .collect(Collectors.toSet());

        for (CategoryDTO parsedCat : parsedTree.getCategories()) {
            // Пропускаем служебную категорию "Все категории".
            if (ALL_CATEGORIES.equals(parsedCat.getName())) continue;
            // новая категория
            if (!dbNames.contains(parsedCat.getName())) {
                diff.getNewCategories().add(parsedCat);
                // ВСЕ её подкатегории — также новые
                for (SubcategoryDTO sub : parsedCat.getSubcategories()) {
                    // Пропускаем пустые и мусорные субкатегории.
                    if (sub.getName() == null || sub.getName().isBlank()) continue;
                    diff.getNewSubcategories().add(
                            new CategoryDiffResult.SubcategoryWithParentDTO(
                                    null,         // parentId = null - новая категория
                                    parsedCat.getName(),  // имя новой категории
                                    sub
                            )
                    );
                }
            }
        }
    }

    private void compareSubcategories(SiteDTO parsedTree, SiteDTO dbTree, CategoryDiffResult diff) {
        // Строим мапу: имя категории - категория из БД.
        // Это нужно, чтобы понять: категория уже существует или она новая с субкатегорией.
        Map<String, CategoryDTO> dbByName = dbTree.getCategories().stream()
                .collect(Collectors.toMap(CategoryDTO::getName, c -> c));
        // Идём по всем категориям, которые пришли из парсеров.
        for (CategoryDTO parsedCat : parsedTree.getCategories()) {
            // Пропускаем служебную категорию "Все категории".
            if (ALL_CATEGORIES.equals(parsedCat.getName())) continue;
            // Пытаемся найти категорию в БД.
            // Если нашли - категория существующая.
            // Если не нашли - категория новая.
            CategoryDTO dbCat = dbByName.get(parsedCat.getName());
            // Если категории НЕТ в БД:
            // значит она новая, и ВСЕ её подкатегории уже были добавлены
            // в compareCategories (там parentId = null).
            // Здесь мы НЕ должны добавлять их повторно.
            if (dbCat == null) continue;
            // Собираем имена подкатегорий, которые уже есть в БД
            // под ЭТОЙ категорией.
            // Это нужно, чтобы понять: субкатегория новая или уже существует.
            Set<String> dbSubs = dbCat.getSubcategories().stream()
                    .map(SubcategoryDTO::getName)
                    .collect(Collectors.toSet());

            // Идём по подкатегориям, которые пришли из парсера.
            for (SubcategoryDTO sub : parsedCat.getSubcategories()) {
                // Пропускаем пустые и мусорные субкатегории.
                if (sub.getName() == null || sub.getName().isBlank()) continue;
                // Если подкатегории НЕТ в БД под этой категорией:
                // значит это НОВАЯ подкатегория к СУЩЕСТВУЮЩЕЙ категории.
                // ЛОГИКА:
                // - категория существует - dbCat != null
                // - субкатегории нет - dbSubs не содержит имя
                // значит нужно создать саб и привязать к существующей категории.
                if (!dbSubs.contains(sub.getName())) {
                    diff.getNewSubcategories().add(
                            new CategoryDiffResult.SubcategoryWithParentDTO(
                                    dbCat.getId(),      // - ID существующей категории
                                    dbCat.getName(),    // - имя существующей категории
                                    sub                 // - новая подкатегория
                            )
                    );
                }
            }
        }
    }

    private CategoryDiffResult compareRemoved(SiteDTO parsedTree, SiteDTO dbTree) {
        CategoryDiffResult diff = new CategoryDiffResult();

        Map<String, CategoryDTO> parsedByName = parsedTree.getCategories().stream()
                .filter(c -> !ALL_CATEGORIES.equals(c.getName()))
                .collect(Collectors.toMap(CategoryDTO::getName, c -> c));

        for (CategoryDTO dbCat : dbTree.getCategories()) {
            if (ALL_CATEGORIES.equals(dbCat.getName())) continue;

            CategoryDTO parsedCat = parsedByName.get(dbCat.getName());

            // Категория удалена
            if (parsedCat == null) {
                diff.getRemovedCategories().add(dbCat);

                for (SubcategoryDTO sub : dbCat.getSubcategories()) {
                    diff.getRemovedSubcategories().add(
                            new CategoryDiffResult.SubcategoryWithParentDTO(
                                    dbCat.getId(),
                                    dbCat.getName(),
                                    sub
                            )
                    );
                }
                continue;
            }

            // Удалённые подкатегории
            Map<String, SubcategoryDTO> parsedSubs = parsedCat.getSubcategories().stream()
                    .collect(Collectors.toMap(SubcategoryDTO::getName, s -> s, (a, b) -> a));

            for (SubcategoryDTO sub : dbCat.getSubcategories()) {
                if (!parsedSubs.containsKey(sub.getName())) {
                    diff.getRemovedSubcategories().add(
                            new CategoryDiffResult.SubcategoryWithParentDTO(
                                    dbCat.getId(),
                                    dbCat.getName(),
                                    sub
                            )
                    );
                }
            }
        }

        return diff;
    }


    private CategoryDiffResult compareMoved(SiteDTO parsedTree, SiteDTO dbTree) {
        CategoryDiffResult diff = new CategoryDiffResult();

        Map<String, String> dbParent = new HashMap<>();
        Map<String, SubcategoryDTO> dbSubsByName = new HashMap<>();

        for (CategoryDTO dbCat : dbTree.getCategories()) {
            for (SubcategoryDTO sub : dbCat.getSubcategories()) {
                dbParent.put(sub.getName(), dbCat.getName());
                dbSubsByName.put(sub.getName(), sub);
            }
        }

        Map<String, String> parsedParent = new HashMap<>();

        for (CategoryDTO parsedCat : parsedTree.getCategories()) {
            for (SubcategoryDTO sub : parsedCat.getSubcategories()) {
                parsedParent.put(sub.getName(), parsedCat.getName());
            }
        }

        Map<String, CategoryDTO> dbByName = dbTree.getCategories().stream()
                .collect(Collectors.toMap(CategoryDTO::getName, c -> c));

        Map<String, CategoryDTO> parsedByName = parsedTree.getCategories().stream()
                .collect(Collectors.toMap(CategoryDTO::getName, c -> c));

        for (String subName : parsedParent.keySet()) {

            SubcategoryDTO oldSub = dbSubsByName.get(subName);

            if (oldSub == null) continue;
            if (oldSub.getId() == null) continue;

            String oldParentName = dbParent.get(subName);
            String newParentName = parsedParent.get(subName);

            if (oldParentName == null || oldParentName.equals(newParentName)) continue;

            CategoryDTO oldParent = dbByName.get(oldParentName);
            CategoryDTO newParent = parsedByName.get(newParentName);

            diff.getMovedSubcategories().add(
                    new CategoryDiffResult.SubcategoryMoveDTO(
                            oldParent.getId(), oldParent.getName(),
                            newParent != null ? newParent.getId() : null,
                            newParentName,
                            oldSub
                    )
            );
        }

        return diff;
    }
}

