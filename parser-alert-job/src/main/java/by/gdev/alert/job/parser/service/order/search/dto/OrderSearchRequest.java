package by.gdev.alert.job.parser.service.order.search.dto;

import lombok.Data;

import java.util.List;

@Data
public class OrderSearchRequest {
    private List<String> sites;
    private String mode; // TITLE or DESCRIPTION
    private List<String> keywords;
    private int page = 0;
    private int size = 20;
}
