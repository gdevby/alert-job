package by.gdev.alert.job.parser.controller;

import by.gdev.alert.job.parser.service.category.update.CategoryUpdateService;
import by.gdev.alert.job.parser.service.category.update.dto.changes.CategoryUpdateSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/category-check")
@RequiredArgsConstructor
public class CategoryCheckController {

    private final CategoryUpdateService categoryUpdateService;

    //Тестовый запуск шедулера для проверки изменений текущей версии сайтов с тем что у нас в базе
    @GetMapping("/update")
    public Map<String, Object> checkOrUpdateAll() {

        CategoryUpdateSummary summary;
        try {
            summary = categoryUpdateService.updateAllSitesWithRetriesAsync().get();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка фонового обновления категорий", e);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("startTime", formatTime(summary.startTime()));
        response.put("endTime", formatTime(summary.endTime()));
        response.put("duration", humanDuration(summary.duration()));
        response.put("attempt", summary.attempts());
        response.put("changes", summary.finalChanges());

        if (summary.finalChanges().isEmpty()) {
            response.put("message", "Изменений нет при обновлении категорий");
        } else {
            response.put("message", "Изменения были найдены");
        }

        return response;
    }

    private String formatTime(long millis) {
        return java.time.Instant.ofEpochMilli(millis)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDateTime()
                .toString()
                .replace('T', ' ');
    }

    private String humanDuration(long ms) {
        if (ms < 1000) return ms + " мс";
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        seconds %= 60;
        minutes %= 60;
        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours).append(" ч ");
        if (minutes > 0) sb.append(minutes).append(" мин ");
        if (seconds > 0) sb.append(seconds).append(" сек");
        return sb.toString().trim();
    }
}
