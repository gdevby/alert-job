package by.gdev.alert.job.parser.controller;

import by.gdev.alert.job.parser.service.category.update.CategoryUpdateService;
import by.gdev.alert.job.parser.service.category.update.dto.changes.CategoryChangeDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/test/category-check")
@RequiredArgsConstructor
public class CategoryCheckController {

    private final CategoryUpdateService categoryUpdateService;

    //Тестовый запуск шедулера для проверки изменений текущей версии сайтов с тем что у нас в базе
    @GetMapping("/update")
    public List<CategoryChangeDTO> checkOrUpdateAll() {
        try {
            return categoryUpdateService.updateAllSitesAsync().get();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка фонового обновления категорий", e);
        }
    }
}
