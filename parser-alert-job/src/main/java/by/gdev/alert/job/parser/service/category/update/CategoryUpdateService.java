package by.gdev.alert.job.parser.service.category.update;

import by.gdev.alert.job.parser.domain.db.SiteSourceJob;
import by.gdev.alert.job.parser.repository.SiteSourceJobRepository;
import by.gdev.alert.job.parser.service.category.CategoryParser;
import by.gdev.alert.job.parser.service.category.ParsedCategory;
import by.gdev.alert.job.parser.service.category.check.CategoryParserFactory;
import by.gdev.alert.job.parser.service.category.update.component.CategoryTreeService;
import by.gdev.alert.job.parser.service.category.update.dto.changes.CategoryChangeDTO;
import by.gdev.alert.job.parser.service.category.update.dto.changes.CategoryDiffDTO;
import by.gdev.alert.job.parser.service.category.check.client.CoreClient;

import by.gdev.alert.job.parser.service.category.update.dto.changes.CategoryDiffResult;
import by.gdev.alert.job.parser.service.category.update.dto.tree.SiteDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryUpdateService {

    private final CategoryDiffApplyService categoryDiffApplyService;
    private final SiteSourceJobRepository siteSourceJobRepository;
    private final CategoryTreeService categoryTreeService;
    private final CategoryParserFactory parserFactory;
    private final CoreClient coreClient;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public Future<List<CategoryChangeDTO>> updateAllSitesAsync() {
        return executor.submit(this::updateAllSites);
    }

    @Transactional
    public List<CategoryChangeDTO> updateAllSites() {
        List<CategoryChangeDTO> changes = new ArrayList<>();

        //Цикл по всем сайтам
        for (SiteSourceJob job : siteSourceJobRepository.findAll()) {
            try {
                //Пытаемся получить изменения в категориях для конкретного сайта и
                // найти изменения с текущей нашей версией и то что сейчас на сайте
                CategoryChangeDTO dto = updateSingleSite(job);
                if (dto != null) {
                    changes.add(dto);
                }
            } catch (Exception e) {
                log.error("Ошибка обновления категорий для сайта {}", job.getName(), e);
            }
        }

        //Если есть изменения в категориях - отправляем эти изменения в core модуль,
        // чтобы обновить их у пользователей сайта
        if (!changes.isEmpty()) {
            coreClient.sendCategoryChanges(changes);
        }
        return changes;
    }

    private CategoryChangeDTO updateSingleSite(SiteSourceJob job) {
        // Дерево из базы
        SiteDTO dbTree = categoryTreeService.buildTree(job);

        // Получаем парсер для сайта
        CategoryParser parser = parserFactory.getParser(job);
        // Парсим сайт
        Map<ParsedCategory, List<ParsedCategory>> parsedMap = parser.parse(job);
        // Если парсер вернул пустой результат (не смог подключиться к сайту или по другой причине - считаем дерево категорий
        // в базе актуальным и пропускаем сравнение деревьев категорий сайта и базы
        if (parsedMap == null || parsedMap.isEmpty()) {
            log.warn("Парсер {} вернул пустой результат. Пропускаем обновление.", job.getName());
            return null;
        }
        //Строим дерево из того что распарсили
        SiteDTO parsedTree = categoryTreeService.buildParsedTree(job, parsedMap);

        // Сравнение деревьев : распаршенного и дерева из базы
        CategoryDiffResult diff = categoryTreeService.compareTrees(parsedTree, dbTree);

        if (diff.isEmpty()) {
            return null;
        }

        // Применяем разницу деревьев распаршенного и из базы в модуле parser
        categoryDiffApplyService.applyDiff(job, diff);

        // Конвертируем в DTO для отправки в core
        CategoryDiffDTO diffDto = buildCoreDto(diff);
        return new CategoryChangeDTO(job.getId(), job.getName(), diffDto);
    }

    private CategoryDiffDTO buildCoreDto(CategoryDiffResult diff) {

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
}
