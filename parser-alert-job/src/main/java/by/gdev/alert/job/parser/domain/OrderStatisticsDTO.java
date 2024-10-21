package by.gdev.alert.job.parser.domain;

import lombok.Data;

import java.time.LocalDate;

@Data
public class OrderStatisticsDTO {
    private Long id;
    private int numberOfOrders;
    private LocalDate date;
}
