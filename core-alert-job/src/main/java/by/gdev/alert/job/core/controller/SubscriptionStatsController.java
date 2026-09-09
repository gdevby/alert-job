package by.gdev.alert.job.core.controller;

import by.gdev.alert.job.core.model.db.DailySubscriptionStat;
import by.gdev.alert.job.core.service.SubscriptionStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stats/subscriptions")
@RequiredArgsConstructor
@Tag(
        name = "Subscription Statistics",
        description = "Статистика подписок по дням, включая последние N дней и последний месяц"
)
public class SubscriptionStatsController {

    private final SubscriptionStatisticsService statsService;

    @Operation(
            summary = "Статистика за последний месяц",
            description = "Возвращает список ежедневной статистики подписок за последние 30 дней."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Список DailySubscriptionStat",
            content = @Content(
                    array = @ArraySchema(schema = @Schema(implementation = DailySubscriptionStat.class))
            )
    )
    @GetMapping("/last-month")
    public List<DailySubscriptionStat> getLastMonthStats() {
        return statsService.getStatsForLastDays(30);
    }

    @Operation(
            summary = "Статистика за последние N дней",
            description = "Возвращает список ежедневной статистики подписок за указанное количество дней."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Список DailySubscriptionStat",
            content = @Content(
                    array = @ArraySchema(schema = @Schema(implementation = DailySubscriptionStat.class))
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Некорректный параметр days"
    )
    @GetMapping("/last-days")
    public List<DailySubscriptionStat> getStatsForLastDays(
            @Parameter(description = "Количество дней для выборки", example = "30")
            @RequestParam(defaultValue = "30") int days
    ) {
        return statsService.getStatsForLastDays(days);
    }
}
