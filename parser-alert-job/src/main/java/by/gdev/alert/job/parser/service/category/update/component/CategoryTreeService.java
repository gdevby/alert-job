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
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static by.gdev.common.model.CategoryLexemes.ALL_CATEGORIES;

@Service
@RequiredArgsConstructor
public class CategoryTreeService {

    private final CategoryRepository categoryRepository;

    public SiteDTO buildTree(SiteSourceJob job) {
        // АВТОЧИСТКА БИТЫХ SUBCATEGORY
        categoryRepository.fixBrokenSubcategoryPositions(job.getId());

        // Загружаем категории + подкатегории одним запросом
        List<Category> categories =
                categoryRepository.findAllWithSubcategoriesBySourceId(job.getId());

        // DTO сайта
        SiteDTO site = new SiteDTO();
        site.setId(job.getId());
        site.setName(job.getName());

        List<CategoryDTO> categoryDTOs = new ArrayList<>();

        for (Category c : categories) {

            CategoryDTO catDto = new CategoryDTO();
            catDto.setId(c.getId());
            catDto.setName(c.getNativeLocName());

            List<SubcategoryDTO> subDTOs = new ArrayList<>();
            if (c.getSubCategories() != null) {
                for (Subcategory s : c.getSubCategories()) {
                    SubcategoryDTO sd = new SubcategoryDTO();
                    sd.setId(s.getId());
                    sd.setName(s.getNativeLocName());
                    subDTOs.add(sd);
                }
            }

            catDto.setSubcategories(subDTOs);
            categoryDTOs.add(catDto);
        }

        site.setCategories(categoryDTOs);
        return site;
    }

    public SiteDTO buildParsedTree(SiteSourceJob job, Map<ParsedCategory, List<ParsedCategory>> parsed) {
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

    public CategoryDiffResult compareTrees(SiteDTO parsedTree, SiteDTO dbTree) {
        CategoryDiffResult diff = new CategoryDiffResult();

        //Категории по имени
        Map<String, CategoryDTO> dbByName = dbTree.getCategories().stream()
                .filter(c -> !ALL_CATEGORIES.equals(c.getName()))
                .collect(Collectors.toMap(CategoryDTO::getName, c -> c));

        Map<String, CategoryDTO> parsedByName = parsedTree.getCategories().stream()
                .filter(c -> !ALL_CATEGORIES.equals(c.getName()))
                .collect(Collectors.toMap(CategoryDTO::getName, c -> c));

        //Новые категории
        for (CategoryDTO parsedCat : parsedTree.getCategories()) {
            if (ALL_CATEGORIES.equals(parsedCat.getName())) continue;
            if (!dbByName.containsKey(parsedCat.getName())) {
                diff.getNewCategories().add(parsedCat);
            }
        }

        //Удалённые категории
        for (CategoryDTO dbCat : dbTree.getCategories()) {
            if (ALL_CATEGORIES.equals(dbCat.getName())) continue;
            if (!parsedByName.containsKey(dbCat.getName())) {
                diff.getRemovedCategories().add(dbCat);
            }
        }

        //Подкатегории
        for (CategoryDTO parsedCat : parsedTree.getCategories()) {
            CategoryDTO dbCat = dbByName.get(parsedCat.getName());

            //Категория новая → все подкатегории новые
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

            Map<String, SubcategoryDTO> dbSubs = dbCat.getSubcategories().stream()
                    .collect(Collectors.toMap(SubcategoryDTO::getName, s -> s));

            Map<String, SubcategoryDTO> parsedSubs = parsedCat.getSubcategories().stream()
                    .collect(Collectors.toMap(SubcategoryDTO::getName, s -> s, (a, b) -> a));

            //Новые подкатегории
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

            //Удалённые подкатегории
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

        //Перемещённые подкатегории
        Map<String, String> dbParent = new HashMap<>();
        for (CategoryDTO dbCat : dbTree.getCategories()) {
            for (SubcategoryDTO sub : dbCat.getSubcategories()) {
                dbParent.put(sub.getName(), dbCat.getName());
            }
        }

        Map<String, String> parsedParent = new HashMap<>();
        for (CategoryDTO parsedCat : parsedTree.getCategories()) {
            for (SubcategoryDTO sub : parsedCat.getSubcategories()) {
                parsedParent.put(sub.getName(), parsedCat.getName());
            }
        }

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

        // === ПРАВИЛО ДЛЯ САЙТОВ БЕЗ ПОДКАТЕГОРИЙ ===
        boolean parsedHasNoSubcategories = parsedTree.getCategories().stream()
                .allMatch(c -> c.getSubcategories().isEmpty());

        if (parsedHasNoSubcategories) {
            // убираем возможные дубли
            diff.getRemovedSubcategories().clear();

            for (CategoryDTO dbCat : dbTree.getCategories()) {
                for (SubcategoryDTO sub : dbCat.getSubcategories()) {
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


    private SubcategoryDTO findSubByName(SiteDTO tree, String name) {
        for (CategoryDTO cat : tree.getCategories()) {
            for (SubcategoryDTO sub : cat.getSubcategories()) {
                if (sub.getName().equals(name)) return sub;
            }
        }
        return null;
    }

}

