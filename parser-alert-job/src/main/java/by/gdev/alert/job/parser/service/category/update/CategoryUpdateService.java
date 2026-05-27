package by.gdev.alert.job.parser.service.category.update;

import by.gdev.alert.job.parser.domain.db.SiteSourceJob;
import by.gdev.alert.job.parser.repository.SiteSourceJobRepository;
import by.gdev.alert.job.parser.service.category.CategoryParser;
import by.gdev.alert.job.parser.service.category.ParsedCategory;
import by.gdev.alert.job.parser.service.category.check.CategoryParserFactory;
import by.gdev.alert.job.parser.service.category.update.component.CategoryTreeService;
import by.gdev.alert.job.parser.service.category.update.dto.ParsedResult;
import by.gdev.alert.job.parser.service.category.update.dto.changes.CategoryChangeDTO;
import by.gdev.alert.job.parser.service.category.update.dto.changes.CategoryDiffDTO;
import by.gdev.alert.job.parser.service.category.check.client.CoreClient;

import by.gdev.alert.job.parser.service.category.update.dto.changes.CategoryDiffResult;
import by.gdev.alert.job.parser.service.category.update.dto.changes.CategoryUpdateSummary;
import by.gdev.alert.job.parser.service.category.update.dto.tree.SiteDTO;
import by.gdev.common.model.SiteName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

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
    //Пул для запуска обновления категорий в отдельном потоке
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    //Пул для парсинга категорий в отдельных потоках - по количеству сайтов
    private final ExecutorService parsePool = Executors.newFixedThreadPool(SiteName.values().length);

    @Value("${category.update.max-iterations:3}")
    private int maxIterations;

    public CategoryUpdateSummary updateAllSitesWithRetries() {
        long start = System.currentTimeMillis();
        int iteration = 0;
        Map<String, CategoryDiffDTO> mergedDiffs = new HashMap<>();
        while (iteration < maxIterations) {
            iteration++;
            List<CategoryChangeDTO> changes = updateAllSites();

            if (changes != null && !changes.isEmpty()) {
                for (CategoryChangeDTO change : changes) {
                    mergedDiffs.merge(
                            change.siteName(),
                            change.diff(),
                            this::mergeDiffs
                    );
                }
            }
            if (changes == null || changes.isEmpty()) {
                break;
            }
        }

        long end = System.currentTimeMillis();
        long duration = end - start;

        List<CategoryChangeDTO> finalList = mergedDiffs.entrySet().stream()
                .map(e -> new CategoryChangeDTO(
                        siteSourceJobRepository.findByName(e.getKey()).getId(),
                        e.getKey(),
                        e.getValue()
                ))
                .toList();

        //Если есть изменения в категориях - отправляем эти изменения в core модуль,
        // чтобы обновить их у пользователей сайта и оповестить администраторов AlertJob
        if (!finalList.isEmpty()) {
            coreClient.sendCategoryChanges(finalList);
        }

        return new CategoryUpdateSummary(
                start,
                end,
                duration,
                iteration,
                finalList
        );
    }

    private CategoryDiffDTO mergeDiffs(CategoryDiffDTO a, CategoryDiffDTO b) {

        a.getNewCategories().addAll(b.getNewCategories());
        a.getRemovedCategories().addAll(b.getRemovedCategories());
        a.getMovedCategories().addAll(b.getMovedCategories());

        a.getNewSubcategories().addAll(b.getNewSubcategories());
        a.getRemovedSubcategories().addAll(b.getRemovedSubcategories());
        a.getMovedSubcategories().addAll(b.getMovedSubcategories());

        return a;
    }

    public Future<CategoryUpdateSummary> updateAllSitesWithRetriesAsync() {
        return executor.submit(this::updateAllSitesWithRetries);
    }

    @Transactional
    public List<CategoryChangeDTO> updateAllSites() {
        List<SiteSourceJob> jobs = new ArrayList<>();
        siteSourceJobRepository.findAll().forEach(jobs::add);

        // Параллельно парсим все сайты
        List<Future<ParsedResult>> futures = new ArrayList<>();
        for (SiteSourceJob job : jobs) {
            futures.add(parsePool.submit(() -> parseSite(job)));
        }

        // Собираем результаты парсинга
        List<ParsedResult> parsedResults = new ArrayList<>();
        for (Future<ParsedResult> f : futures) {
            try {
                ParsedResult r = f.get();
                if (r != null) parsedResults.add(r);
            } catch (Exception e) {
                log.error("Ошибка парсинга сайта", e);
            }
        }

        // Теперь последовательно применяем diff
        List<CategoryChangeDTO> changes = new ArrayList<>();
        for (ParsedResult result : parsedResults) {
            try {
                CategoryChangeDTO dto = applyParsedTree(result);
                if (dto != null) changes.add(dto);
            } catch (Exception e) {
                log.error("Ошибка применения diff для {}", result.job().getName(), e);
            }
        }
        return changes;
    }

    private ParsedResult parseSite(SiteSourceJob job) {
        try {
            CategoryParser parser = parserFactory.getParser(job);
            if (parser == null) {
                log.warn("Парсер для {} отсутствует", job.getName());
                return null;
            }

            Map<ParsedCategory, List<ParsedCategory>> parsedMap = parser.parse(job);
            if (parsedMap == null || parsedMap.isEmpty()) {
                log.warn("Парсер {} вернул пустой результат", job.getName());
                return null;
            }

            SiteDTO parsedTree = categoryTreeService.buildParsedTree(job, parsedMap);
            if (parsedTree.getCategories().isEmpty()) {
                log.warn("ParsedTree пустой {}", job.getName());
                return null;
            }

            return new ParsedResult(job, parsedTree);

        } catch (Exception e) {
            log.error("Ошибка парсинга {}", job.getName(), e);
            return null;
        }
    }

    private CategoryChangeDTO applyParsedTree(ParsedResult result) {
        SiteSourceJob job = result.job();
        SiteDTO parsedTree = result.parsedTree();

        SiteDTO dbTree = categoryTreeService.buildTree(job);
        CategoryDiffResult diff = categoryTreeService.compareTrees(parsedTree, dbTree);

        diff.getNewSubcategories().removeIf(s ->
                s.getSubcategory().getName() == null ||
                        s.getSubcategory().getName().isBlank()
        );

        if (!diff.isEmpty()) {
            categoryDiffApplyService.applyDiff(job, diff);
        }
        categoryDiffApplyService.applyOrder(job, parsedTree);
        if (diff.isEmpty()) return null;
        CategoryDiffDTO diffDto = buildCoreDto(diff);
        return new CategoryChangeDTO(job.getId(), job.getName(), diffDto);
    }

    private CategoryDiffDTO buildCoreDto(CategoryDiffResult diff) {

        diff.getNewSubcategories().removeIf(s ->
                s.getSubcategory().getName() == null ||
                        s.getSubcategory().getName().isBlank()
        );

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
