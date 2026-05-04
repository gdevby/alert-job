package by.gdev.alert.job.core.controller;

import by.gdev.alert.job.core.configuration.category.AdminProperties;
import by.gdev.alert.job.core.model.category.CategoryChangeListDTO;
import by.gdev.alert.job.core.model.db.AppUser;
import by.gdev.alert.job.core.repository.AppUserRepository;
import by.gdev.alert.job.core.service.MailSenderService;
import by.gdev.common.model.NotificationType;
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

    private final AdminProperties adminProperties;
    private final AppUserRepository userRepository;
    private final MailSenderService mailSenderService;

    @PostMapping("/changes")
    public void receiveChanges(@RequestBody CategoryChangeListDTO dto) {

        if (dto.changes().isEmpty()) {
            return;
        }

        AppUser admin = userRepository.findByUuid(adminProperties.getUuid())
                .orElseThrow(() -> new IllegalStateException("Admin not found"));

        String html = buildCategoryDiffHtml(dto.changes());

        mailSenderService.sendMessagesToUser(
                admin,
                List.of(html),
                NotificationType.CATEGORY_CHANGE
        );
    }
}

