package by.gdev.alert.job.parser.controller;

import by.gdev.alert.job.parser.service.ParserService;
import by.gdev.common.model.CategoryDTO;
import by.gdev.common.model.OrderDTO;
import by.gdev.common.model.SiteSourceDTO;
import by.gdev.common.model.SubCategoryDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST API парсера: категории, подкатегории, подписки и просмотр сохранённых заказов.
 * Парсинг бирж выполняется только через scheduler ({@link by.gdev.alert.job.parser.scheduller.parser.SiteParserScheduler}).
 */
@RestController
@RequestMapping("/api/")
@RequiredArgsConstructor
@Slf4j
public class OrderParserController {

    private final ParserService parserService;

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
