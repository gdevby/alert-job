package by.gdev.alert.job.core.controller;

import by.gdev.alert.job.core.configuration.category.AdminProperties;
import by.gdev.alert.job.core.model.category.CategoryChangeDTO;
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

        String html = buildHtml(dto.changes());

        mailSenderService.sendMessagesToUser(
                admin,
                List.of(html),
                NotificationType.CATEGORY_CHANGE
        );
    }

    private String buildHtml(List<CategoryChangeDTO> changes) {
        StringBuilder sb = new StringBuilder();

        sb.append("<h2>Изменения категорий</h2>");

        for (CategoryChangeDTO change : changes) {
            sb.append("<h3>Сайт: ").append(change.siteName()).append("</h3>");

            var diff = change.diff();

            if (!diff.getNewCategories().isEmpty()) {
                sb.append("<p><b>Новые категории:</b></p><ul>");
                diff.getNewCategories().forEach(c -> sb.append("<li>").append(c).append("</li>"));
                sb.append("</ul>");
            }

            if (!diff.getRemovedCategories().isEmpty()) {
                sb.append("<p><b>Удалённые категории:</b></p><ul>");
                diff.getRemovedCategories().forEach(c -> sb.append("<li>").append(c).append("</li>"));
                sb.append("</ul>");
            }

            if (!diff.getNewSubcategories().isEmpty()) {
                sb.append("<p><b>Новые подкатегории:</b></p><ul>");
                diff.getNewSubcategories().forEach((parent, subs) -> {
                    subs.forEach(sub -> sb.append("<li>")
                            .append(parent).append(" → ").append(sub)
                            .append("</li>"));
                });
                sb.append("</ul>");
            }

            if (!diff.getRemovedSubcategories().isEmpty()) {
                sb.append("<p><b>Удалённые подкатегории:</b></p><ul>");
                diff.getRemovedSubcategories().forEach((parent, subs) -> {
                    subs.forEach(sub -> sb.append("<li>")
                            .append(parent).append(" → ").append(sub)
                            .append("</li>"));
                });
                sb.append("</ul>");
            }

            sb.append("<hr>");
        }

        return sb.toString();
    }

}
