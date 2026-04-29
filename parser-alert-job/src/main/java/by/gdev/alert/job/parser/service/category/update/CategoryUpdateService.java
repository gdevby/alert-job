package by.gdev.alert.job.parser.service.category.update;

import by.gdev.alert.job.parser.domain.db.Category;
import by.gdev.alert.job.parser.domain.db.SiteSourceJob;
import by.gdev.alert.job.parser.domain.db.Subcategory;
import by.gdev.alert.job.parser.repository.CategoryRepository;
import by.gdev.alert.job.parser.repository.SiteSourceJobRepository;
import by.gdev.alert.job.parser.repository.SubCategoryRepository;
import by.gdev.alert.job.parser.service.category.CategoryParser;
import by.gdev.alert.job.parser.service.category.ParsedCategory;
import by.gdev.alert.job.parser.service.category.check.CategoryParserFactory;
import by.gdev.alert.job.parser.service.category.update.component.CategoryTreeService;
import by.gdev.alert.job.parser.service.category.update.dto.changes.CategoryChangeDTO;
import by.gdev.alert.job.parser.service.category.update.dto.changes.CategoryDiffDTO;
import by.gdev.alert.job.parser.service.category.check.client.CoreClient;

import by.gdev.alert.job.parser.service.category.update.dto.changes.CategoryDiffResult;
import by.gdev.alert.job.parser.service.category.update.dto.tree.CategoryDTO;
import by.gdev.alert.job.parser.service.category.update.dto.tree.SiteDTO;
import by.gdev.alert.job.parser.service.category.update.dto.tree.SubcategoryDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryUpdateService {

    private final SiteSourceJobRepository siteSourceJobRepository;
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final CategoryTreeService categoryTreeService;
    private final CategoryParserFactory parserFactory;
    private final CoreClient coreClient;

    @Transactional
    public List<CategoryChangeDTO> updateAllSites() {
        List<CategoryChangeDTO> changes = new ArrayList<>();

        for (SiteSourceJob job : siteSourceJobRepository.findAll()) {
            try {
                CategoryChangeDTO dto = updateSingleSite(job);
                if (dto != null) {
                    changes.add(dto);
                }
            } catch (Exception e) {
                log.error("Ошибка обновления категорий для сайта {}", job.getName(), e);
            }
        }

        /*if (!changes.isEmpty()) {
            coreClient.sendCategoryChanges(changes);
        }*/

        return changes;
    }

    private CategoryChangeDTO updateSingleSite(SiteSourceJob job) throws IOException {

        // Дерево из базы
        SiteDTO dbTree = categoryTreeService.buildTree(job);

        // Дерево из парсера
        CategoryParser parser = parserFactory.getParser(job);
        SiteDTO parsedTree = buildParsedTree(job, parser.parse(job));

        // 3. Сравнение
        CategoryDiffResult diff = compareTrees(parsedTree, dbTree);

        if (diff.isEmpty()) {
            return null;
        }

        // 4. Применяем diff
        //applyDiff(job, diff);

        // 5. Конвертируем в DTO для core
        CategoryDiffDTO diffDto = toDto(diff);

        return new CategoryChangeDTO(job.getName(), diffDto);
    }


    private CategoryDiffResult compareTrees(SiteDTO parsedTree, SiteDTO dbTree) {

        CategoryDiffResult diff = new CategoryDiffResult();

        // --- 1. Категории по имени ---
        Map<String, CategoryDTO> dbByName = dbTree.getCategories().stream()
                .collect(Collectors.toMap(CategoryDTO::getName, c -> c));

        Map<String, CategoryDTO> parsedByName = parsedTree.getCategories().stream()
                .collect(Collectors.toMap(CategoryDTO::getName, c -> c));

        // --- Новые категории ---
        for (CategoryDTO parsedCat : parsedTree.getCategories()) {
            if (!dbByName.containsKey(parsedCat.getName())) {
                diff.getNewCategories().add(parsedCat);
            }
        }

        // --- Удалённые категории ---
        for (CategoryDTO dbCat : dbTree.getCategories()) {
            if (!parsedByName.containsKey(dbCat.getName())) {
                diff.getRemovedCategories().add(dbCat);
            }
        }

        // --- 2. Подкатегории ---
        // Для каждой категории, которая есть в parsedTree
        for (CategoryDTO parsedCat : parsedTree.getCategories()) {

            CategoryDTO dbCat = dbByName.get(parsedCat.getName());

            // Категория новая → все подкатегории новые
            if (dbCat == null) {
                for (SubcategoryDTO sub : parsedCat.getSubcategories()) {
                    diff.getNewSubcategories().add(
                            new CategoryDiffResult.SubcategoryWithParentDTO(
                                    null,
                                    parsedCat.getName(),
                                    sub
                            )
                    );
                }
                continue;
            }

            // Подкатегории по имени внутри категории
            Map<String, SubcategoryDTO> dbSubs = dbCat.getSubcategories().stream()
                    .collect(Collectors.toMap(SubcategoryDTO::getName, s -> s));

            Map<String, SubcategoryDTO> parsedSubs = parsedCat.getSubcategories().stream()
                    .collect(Collectors.toMap(SubcategoryDTO::getName, s -> s));

            // Новые подкатегории
            for (SubcategoryDTO sub : parsedCat.getSubcategories()) {
                if (!dbSubs.containsKey(sub.getName())) {
                    diff.getNewSubcategories().add(
                            new CategoryDiffResult.SubcategoryWithParentDTO(
                                    dbCat.getId(),
                                    dbCat.getName(),
                                    sub
                            )
                    );
                }
            }

            // Удалённые подкатегории
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

        // --- 3. Перемещённые подкатегории ---
        // subName → parentName (db)
        Map<String, String> dbParent = new HashMap<>();
        for (CategoryDTO dbCat : dbTree.getCategories()) {
            for (SubcategoryDTO sub : dbCat.getSubcategories()) {
                dbParent.put(sub.getName(), dbCat.getName());
            }
        }

        // subName → parentName (parsed)
        Map<String, String> parsedParent = new HashMap<>();
        for (CategoryDTO parsedCat : parsedTree.getCategories()) {
            for (SubcategoryDTO sub : parsedCat.getSubcategories()) {
                parsedParent.put(sub.getName(), parsedCat.getName());
            }
        }

        // сравниваем
        for (Map.Entry<String, String> e : parsedParent.entrySet()) {

            String subName = e.getKey();
            String newParentName = e.getValue();
            String oldParentName = dbParent.get(subName);

            if (oldParentName != null && !oldParentName.equals(newParentName)) {

                CategoryDTO oldParent = dbByName.get(oldParentName);
                CategoryDTO newParent = parsedByName.get(newParentName);

                SubcategoryDTO sub = findSubByName(parsedTree, subName);

                diff.getMovedSubcategories().add(
                        new CategoryDiffResult.SubcategoryMoveDTO(
                                oldParent.getId(), oldParent.getName(),
                                newParent.getId(), newParent.getName(),
                                sub
                        )
                );
            }
        }

        return diff;
    }

    private SubcategoryDTO findSubByName(SiteDTO tree, String name) {
        for (CategoryDTO cat : tree.getCategories()) {
            for (SubcategoryDTO sub : cat.getSubcategories()) {
                if (sub.getName().equals(name)) return sub;
            }
        }
        return null;
    }



    private CategoryDiffDTO toDto(CategoryDiffResult diff) {

        CategoryDiffDTO dto = new CategoryDiffDTO();

        // Категории
        dto.setNewCategories(diff.getNewCategories());
        dto.setRemovedCategories(diff.getRemovedCategories());
        dto.setMovedCategories(diff.getMovedCategories());

        // Подкатегории
        dto.setNewSubcategories(diff.getNewSubcategories());
        dto.setRemovedSubcategories(diff.getRemovedSubcategories());
        dto.setMovedSubcategories(diff.getMovedSubcategories());

        return dto;
    }


    private SiteDTO buildParsedTree(SiteSourceJob job,
                                    Map<ParsedCategory, List<ParsedCategory>> parsed) {

        SiteDTO site = new SiteDTO();
        site.setId(job.getId());
        site.setName(job.getName());

        List<CategoryDTO> categoryDTOs = new ArrayList<>();

        for (Map.Entry<ParsedCategory, List<ParsedCategory>> entry : parsed.entrySet()) {

            ParsedCategory parsedCat = entry.getKey();
            List<ParsedCategory> parsedSubs = entry.getValue();

            // Категория
            CategoryDTO catDto = new CategoryDTO();
            catDto.setName(parsedCat.translatedName());  // имя категории

            List<SubcategoryDTO> subDtos = new ArrayList<>();

            // Подкатегории
            for (ParsedCategory sub : parsedSubs) {
                SubcategoryDTO sd = new SubcategoryDTO();
                sd.setName(sub.translatedName());
                subDtos.add(sd);
            }

            catDto.setSubcategories(subDtos);
            categoryDTOs.add(catDto);
        }

        site.setCategories(categoryDTOs);
        return site;
    }
}
