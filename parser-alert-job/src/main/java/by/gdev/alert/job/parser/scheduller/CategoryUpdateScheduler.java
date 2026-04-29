package by.gdev.alert.job.parser.scheduller;

import by.gdev.alert.job.parser.service.category.update.CategoryUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CategoryUpdateScheduler {

    private final CategoryUpdateService categoryUpdateService;

    @Scheduled(cron = "0 0 3 * * MON") // каждый понедельник в 03:00
    //@Scheduled(cron = "0 */5 * * * *")
    public void weeklyCategoryUpdate() {
        log.info("Запуск еженедельного обновления категорий...");
        categoryUpdateService.updateAllSites();
    }
}
