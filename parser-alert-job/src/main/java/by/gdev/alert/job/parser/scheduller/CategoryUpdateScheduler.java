package by.gdev.alert.job.parser.scheduller;

import by.gdev.alert.job.parser.service.category.update.CategoryUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CategoryUpdateScheduler {

    //Возможность включить или выключить шедулер который "мониторит" изменения в категориях сайтов
    @Value("${scheduler.categories.update.enabled:true}")
    private boolean categoriesSchedulerEnabled;

    private final CategoryUpdateService categoryUpdateService;

    @Scheduled(cron = "0 0 3 * * MON") // каждый понедельник в 03:00
    //@Scheduled(cron = "0 */5 * * * *") //каждые 5 минут
    public void weeklyCategoryUpdate() {
        if (categoriesSchedulerEnabled) {
            log.debug("Запуск ЕЖЕНЕДЕЛЬНОЙ проверки акуальности категорий на сайтах...");
            categoryUpdateService.updateAllSitesWithRetries();
        }
        else {
            log.debug("Шедулер проверки категорий ОТКЛЮЧЕН");
        }
    }
}
