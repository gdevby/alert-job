package by.gdev.alert.job.parser.controller;

import by.gdev.alert.job.parser.service.category.cleanup.CategoriesCleanupComponent;
import by.gdev.alert.job.parser.service.category.cleanup.CleanupMode;
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
            @RequestParam String site,
            @RequestParam CleanupMode mode) {

        categoriesCleanupComponent.cleanup(List.of(site), mode);

        return ResponseEntity.ok(Map.of(
                "message", "Очистка завершена",
                "site", site,
                "mode", mode.toString()
        ));
    }

}
