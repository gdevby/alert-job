package by.gdev.alert.job.core.controller;

import by.gdev.alert.job.core.model.cleanup.CleanupRequest;
import by.gdev.alert.job.core.service.cleanup.CleanupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.util.Map;

@RestController
@RequestMapping("/api/cleanup")
@Slf4j
public class CleanupController {

    @Autowired
    private CleanupService cleanupService;

    @PostMapping(consumes = "application/json", produces = "application/json")
    public ResponseEntity<Map<String, Serializable>> cleanupParserSourceForSite(
            @RequestBody CleanupRequest request) {

        cleanupService.cleanupParserSourceForSite(
                request.siteId(),
                request.siteName(),
                request.categories()
        );

        return ResponseEntity.ok(Map.of(
                "message", "Очистка завершена",
                "site", request.siteId()
        ));
    }

}
