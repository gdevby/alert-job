package by.gdev.alert.job.parser.controller;

import by.gdev.alert.job.parser.service.category.cleanup.CategoriesCleanupComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/parser/cleanup")
@Slf4j
public class CleanupController {

    @Autowired
    CategoriesCleanupComponent categoriesCleanupComponent;

    @PostMapping(produces = "application/json")
    public ResponseEntity<Map<String, Serializable>> cleanupForSite(
            @RequestParam String site) {
        categoriesCleanupComponent.cleanup(List.of(site));
        return ResponseEntity.ok(Map.of(
                "message", "Очистка завершена",
                "site", site
        ));
    }
}
