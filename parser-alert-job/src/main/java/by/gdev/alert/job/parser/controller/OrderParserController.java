package by.gdev.alert.job.parser.controller;

import by.gdev.alert.job.parser.service.ParserService;
import by.gdev.alert.job.parser.service.metrics.MetricsService;
import by.gdev.alert.job.parser.service.order.SiteParser;
import by.gdev.alert.job.parser.service.statistic.StatisticsService;
import by.gdev.common.model.CategoryDTO;
import by.gdev.common.model.OrderDTO;
import by.gdev.common.model.SiteSourceDTO;
import by.gdev.common.model.SubCategoryDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@RestController
@RequestMapping("/api/")
@RequiredArgsConstructor
@Slf4j
public class OrderParserController {

    @Autowired
    private List<SiteParser> parsers;
    private final StatisticsService statisticsService;
    private final ParserService parserService;
    private final MetricsService metricsService;
    private ExecutorService executor = Executors.newCachedThreadPool();

    @GetMapping("/stream-orders")
    public List<OrderDTO> ordersEvents() {
        log.trace("subscribed on orders");

        List<Future<List<OrderDTO>>> futures = parsers.stream()
                .map(parser -> (Callable<List<OrderDTO>>) parser::parse)
                .map(executor::submit)
                .toList();

        List<OrderDTO> allOrders = new ArrayList<>();
        for (int i = 0; i < futures.size(); i++) {
            Future<List<OrderDTO>> future = futures.get(i);
            SiteParser parser = parsers.get(i);

            try {
                List<OrderDTO> orders = future.get();
                allOrders.addAll(orders);

                metricsService.save(parser.getSiteName(), orders.size());
                statisticsService.save(parser.getSiteName(), orders.size());
            } catch (InterruptedException | ExecutionException e) {
                log.error("Error parsing site {}: {}", parser.getSiteName(), e.getMessage());
            }
        }

        return allOrders;
    }

    @GetMapping("sites")
    public List<SiteSourceDTO> sites() {
        return parserService.getSites();
    }

    @GetMapping("categories")
    public List<CategoryDTO> categories(@RequestParam("site_id") Long site) {
        return parserService.getCategories(site);
    }

    @GetMapping("subcategories")
    public List<SubCategoryDTO> subCategories(@RequestParam("category_id") Long category) {
        return parserService.getSubCategories(category);
    }

    @GetMapping("site/{id}")
    public SiteSourceDTO site(@PathVariable("id") Long id) {
        return parserService.getSite(id);
    }

    @GetMapping("site/{id}/category/{category_id}")
    public CategoryDTO category(@PathVariable("id") Long id, @PathVariable("category_id") Long cId) {
        return parserService.getCategory(id, cId);
    }

    @GetMapping("category/{id}/subcategory/{sub_id}")
    public SubCategoryDTO subCategory(@PathVariable("id") Long id, @PathVariable("sub_id") Long subId) {
        return parserService.getSubCategory(id, subId);
    }

    @PatchMapping("subscribe/sources")
    public void subscribeSources(@RequestParam("category_id") Long categoryId,
                                 @RequestParam(name = "subcategory_id", required = false) Long subCategoryId,
                                 @RequestParam("category_value") boolean cValue,
                                 @RequestParam(name = "subcategory_value", required = false) boolean sValue) {
        parserService.subcribeOnSource(categoryId, subCategoryId, cValue, sValue);
    }

    @GetMapping("orders")
    public List<OrderDTO> showOrdersBySource(@RequestParam("site_id") Long site,
                                             @RequestParam("category_id") Long category, @RequestParam(name = "sub_id", required = false) Long subId,
                                             @RequestParam("period") Long period) {
        return parserService.getOrdersBySource(site, category, subId, period);
    }
}