package by.gdev.alert.job.parser.controller;

import by.gdev.alert.job.parser.domain.OrderStatisticsDTO;
import by.gdev.alert.job.parser.domain.db.OrderStatistics;
import by.gdev.alert.job.parser.service.statistic.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class StatisticsController {
    private static final String WEEK = "7";

    private final StatisticsService statisticsService;
    private final ModelMapper mapper;

    @GetMapping("/orders/statistics")
    public Map<String, List<OrderStatisticsDTO>> getOrderInfo(@RequestParam(value = "period", defaultValue = WEEK) int period){
        List<OrderStatistics> statistics = statisticsService.getStatistics(period);
        return statistics.stream()
                .collect(Collectors.groupingBy(
                        orderStatistics -> orderStatistics.getSourceSite().getName(),
                        Collectors.mapping(orderStatistics -> mapper.map(orderStatistics, OrderStatisticsDTO.class), Collectors.toList())
                ));
    }
}
