package by.gdev.alert.job.core.controller;

import by.gdev.alert.job.core.model.db.DailySubscriptionStat;
import by.gdev.alert.job.core.service.SubscriptionStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/admin/stats/subscriptions")
@RequiredArgsConstructor
public class SubscriptionStatsController {

    private final SubscriptionStatisticsService statsService;

    @GetMapping("/last-month")
    public List<DailySubscriptionStat> getLastMonthStats() {
        return statsService.getStatsForLastDays(30);
    }

    @GetMapping("/last-days")
    public List<DailySubscriptionStat> getStatsForLastDays(@RequestParam(defaultValue = "30") int days) {
        return statsService.getStatsForLastDays(days);
    }
}