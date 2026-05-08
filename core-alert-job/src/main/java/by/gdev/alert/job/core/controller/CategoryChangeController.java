package by.gdev.alert.job.core.controller;


import by.gdev.alert.job.core.model.category.CategoryChangeListDTO;
import by.gdev.alert.job.core.service.change.CategoryChangeNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static by.gdev.alert.job.core.templates.MessageTemplates.CategoryDiff.buildCategoryDiffHtml;

@RestController
@RequestMapping("/category")
@RequiredArgsConstructor
public class CategoryChangeController {

    private final CategoryChangeNotificationService categoryChangeNotificationService;

    @PostMapping("/changes")
    public void receiveChanges(@RequestBody CategoryChangeListDTO dto) {
        if (!dto.changes().isEmpty()) {
            categoryChangeNotificationService.performChanges(dto.changes());
        }
    }
}

