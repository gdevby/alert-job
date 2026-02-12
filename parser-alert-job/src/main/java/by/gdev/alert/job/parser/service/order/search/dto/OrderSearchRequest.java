package by.gdev.alert.job.parser.service.order.search.dto;

import lombok.Data;

import java.util.List;

@Data
public class OrderSearchRequest {
    private String site;
    private Long categoryId;
    private Long subCategoryId;
    private List<String> keywords;
    private int page = 0;
    private int size = 20;
}
