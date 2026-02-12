package by.gdev.alert.job.parser.controller;

import by.gdev.alert.job.parser.service.order.search.CategoryService;
import by.gdev.alert.job.parser.service.order.search.dto.OrderSearchRequest;
import by.gdev.alert.job.parser.service.order.search.OrderSearchService;
import by.gdev.alert.job.parser.service.order.search.dto.PageResponse;
import by.gdev.common.model.CategoryDTO;
import by.gdev.common.model.OrderDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderSearchController {

    private final OrderSearchService orderSearchService;
    private final CategoryService categoryService;

    @PostMapping("/search")
    public PageResponse<OrderDTO> search(@RequestBody OrderSearchRequest request) {
        return orderSearchService.search(request);
    }

    @GetMapping("/filters/{site}")
    public List<CategoryDTO> getFilters(@PathVariable String site) {
        return categoryService.getCategoryTree(site);
    }
}
