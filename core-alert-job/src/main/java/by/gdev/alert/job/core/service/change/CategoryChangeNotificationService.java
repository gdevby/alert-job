package by.gdev.alert.job.core.service.change;

import by.gdev.alert.job.core.configuration.category.AdminProperties;
import by.gdev.alert.job.core.model.category.CategoryChangeDTO;
import by.gdev.alert.job.core.model.category.CategoryDiffDTO;
import by.gdev.alert.job.core.model.category.tree.CategoryDTO;
import by.gdev.alert.job.core.model.db.AppUser;
import by.gdev.alert.job.core.model.db.OrderModules;
import by.gdev.alert.job.core.model.db.SourceSite;
import by.gdev.alert.job.core.repository.AppUserRepository;
import by.gdev.alert.job.core.repository.OrderModulesRepository;
import by.gdev.alert.job.core.repository.SourceSiteRepository;
import by.gdev.alert.job.core.service.MailSenderService;
import by.gdev.alert.job.core.service.change.dto.ModuleInfo;
import by.gdev.alert.job.core.service.change.dto.RemovedCategoryInfo;
import by.gdev.alert.job.core.service.change.dto.SiteInfo;
import by.gdev.alert.job.core.service.change.dto.UserInfo;
import by.gdev.alert.job.core.templates.MessageTemplates;
import by.gdev.common.model.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryChangeNotificationService {

    private final MailSenderService mailSenderService;
    private final AdminProperties adminProperties;
    private final AppUserRepository userRepository;
    private final SourceSiteRepository sourceSiteRepository;
    private final OrderModulesRepository orderModulesRepository;

    private void notifyChanges(List<CategoryChangeDTO> changesRequest, List<UserInfo> usersInfo, AppUser adminUser) {
        String message;
        if (adminUser.isDefaultSendType()) {
            // для почты
            message = MessageTemplates.CategoryDiff.buildCategoryDiffHtml(changesRequest, usersInfo);
        } else {
            // для Telegram
            message = MessageTemplates.CategoryDiff.buildCategoryDiffText(changesRequest, usersInfo);
        }
        mailSenderService.sendMessagesToUser(
                adminUser,
                List.of(message),
                NotificationType.CATEGORY_CHANGE
        );
    }

    private void notifyUsers(List<UserInfo> usersInfo) {
        for (UserInfo ui : usersInfo) {
            AppUser user = ui.user();
            String html = MessageTemplates.CategoryDiff.buildUserNotificationHtml(ui);
            mailSenderService.sendRequiredMessagesToUser(
                    user,
                    List.of(html),
                    NotificationType.CATEGORY_CHANGE_USER
            );
        }
    }

    public List<UserInfo> buildUserInfo(List<CategoryChangeDTO> changes) {

        List<UserInfo> result = new ArrayList<>();

        // Собираем структуру Пользователь - Сайт - Модуль - Удаляемые категории
        Map<AppUser, Map<String, Map<String, List<RemovedCategoryInfo>>>> map = new HashMap<>();
        for (CategoryChangeDTO change : changes) {

            Long siteSourceId = change.siteSourceId();
            String siteName = change.siteName();
            CategoryDiffDTO diff = change.diff();

            // ID → название удалённых категорий
            Map<Long, String> removedCategoryNames = diff.getRemovedCategories().stream()
                    .collect(Collectors.toMap(CategoryDTO::getId, CategoryDTO::getName));

            // ID → название удалённых подкатегорий
            Map<Long, String> removedSubcategoryNames = diff.getRemovedSubcategories().stream()
                    .collect(Collectors.toMap(
                            s -> s.getSubcategory().getId(),
                            s -> s.getSubcategory().getName()
                    ));

            // ID подкатегории → имя родительской категории
            Map<Long, String> removedSubcategoryParentNames = diff.getRemovedSubcategories().stream()
                    .collect(Collectors.toMap(
                            s -> s.getSubcategory().getId(),
                            s -> s.getParentName()
                    ));

            // Находим SourceSite
            List<SourceSite> affectedSources = Stream.concat(
                    sourceSiteRepository.findBySiteSourceAndSiteCategoryIn(
                            siteSourceId, removedCategoryNames.keySet()).stream(),
                    sourceSiteRepository.findBySiteSourceAndSiteSubCategoryIn(
                            siteSourceId, removedSubcategoryNames.keySet()).stream()
            ).distinct().toList();

            for (SourceSite source : affectedSources) {
                Long catId = source.getSiteCategory();
                Long subId = source.getSiteSubCategory();

                String categoryName = removedCategoryNames.get(catId);
                String subcategoryName = removedSubcategoryNames.get(subId);

                RemovedCategoryInfo info;

                if (categoryName != null) {
                    // УДАЛЕНА КАТЕГОРИЯ
                    boolean categoryHadSubcategories = subId != null;
                    if (categoryHadSubcategories) {
                        // Категория имела подкатегории
                        info = new RemovedCategoryInfo(categoryName, "Все субкатегории");
                    } else {
                        // Категория без подкатегорий
                        info = new RemovedCategoryInfo(categoryName, null);
                    }

                } else if (subcategoryName != null) {
                    // УДАЛЕНА ПОДКАТЕГОРИЯ
                    String parentName = removedSubcategoryParentNames.get(subId);
                    info = new RemovedCategoryInfo(parentName, subcategoryName);

                } else {
                    info = new RemovedCategoryInfo("Неизвестная категория", null);
                }

                List<AppUser> users = userRepository.findUsersBySourceSiteId(source.getId());
                for (AppUser user : users) {
                    List<OrderModules> modules =
                            orderModulesRepository.findByUserIdAndSources_Id(user.getId(), source.getId());
                    for (OrderModules module : modules) {
                        map
                                .computeIfAbsent(user, u -> new HashMap<>())
                                .computeIfAbsent(siteName, s -> new HashMap<>())
                                .computeIfAbsent(module.getName(), m -> new ArrayList<>())
                                .add(info);
                    }
                }
            }
        }

        // Преобразуем map в DTO
        for (var entryUser : map.entrySet()) {
            AppUser user = entryUser.getKey();
            List<SiteInfo> sites = new ArrayList<>();

            for (var entrySite : entryUser.getValue().entrySet()) {

                String siteName = entrySite.getKey();
                List<ModuleInfo> modules = new ArrayList<>();

                for (var entryModule : entrySite.getValue().entrySet()) {

                    String moduleName = entryModule.getKey();
                    List<RemovedCategoryInfo> removed = entryModule.getValue();

                    modules.add(new ModuleInfo(moduleName, removed));
                }

                sites.add(new SiteInfo(siteName, modules));
            }
            result.add(new UserInfo(user, sites));
        }
        return result;
    }


    public void performChanges(List<CategoryChangeDTO> changesRequest) {
        // Собираем структуру Пользователь - Сайт - Модуль - Удаляемые категории
        List<UserInfo> usersInfo = buildUserInfo(changesRequest);

        // Логируем
        if (!usersInfo.isEmpty()) {
            log.debug("=== Users with removed category filters ===");
            for (UserInfo ui : usersInfo) {
                log.debug("User: {}", ui.user().getEmail());
                for (SiteInfo si : ui.sites()) {
                    log.debug("  Site: {}", si.siteName());
                    for (ModuleInfo mi : si.modules()) {
                        log.debug("    Module: {}", mi.moduleName());
                        for (RemovedCategoryInfo rc : mi.removed()) {
                            log.debug("      Removed: {} -> {}", rc.categoryName(), rc.subcategoryName());
                        }
                    }
                }
            }
            log.debug("==========================================");
        } else {
            log.debug("No users affected by removed categories.");
        }
        //Находим админа
        AppUser adminUser = userRepository.findByUuid(adminProperties.getUuid())
                .orElseThrow(() -> new IllegalStateException("user with role Admin not found"));
        //Отправляем письмо админу
        notifyChanges(changesRequest, usersInfo, adminUser);
        //Уведомляем пользователей
        notifyUsers(usersInfo);
    }

}
