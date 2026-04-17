package by.gdev.alert.job.core.service.cleanup;

import by.gdev.alert.job.core.model.db.AppUser;
import by.gdev.alert.job.core.model.db.OrderModules;
import by.gdev.alert.job.core.model.db.SourceSite;
import by.gdev.alert.job.core.repository.OrderModulesRepository;
import by.gdev.alert.job.core.repository.SourceSiteRepository;
import by.gdev.alert.job.core.service.MailSenderService;
import by.gdev.alert.job.core.service.cleanup.components.ModuleLookupService;
import by.gdev.alert.job.core.service.cleanup.components.WordCleanupRepositoryService;
import by.gdev.common.model.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@RequiredArgsConstructor
@Component
@Slf4j
public class CleanupService {

    private final SourceSiteRepository sourceSiteRepository;
    private final OrderModulesRepository orderModulesRepository;
    private final MailSenderService mailSenderService;
    private final WordCleanupRepositoryService wordCleanupRepositoryService;
    private final ModuleLookupService moduleLookupService;
    private final JdbcTemplate jdbc;

    @Transactional
    public void cleanupParserSourceForSite(Long siteId, String siteName) {

        List<SourceSite> sources = sourceSiteRepository.findAllBySiteSource(siteId);
        log.debug("Found {} SOURCES", sources.size());

        List<Long> wordSiteIds = sources.stream()
                .flatMap(s -> wordCleanupRepositoryService.getTitleWordIds(s.getId()).stream())
                .distinct()
                .toList();

        log.debug("Word size {}", wordSiteIds.size());

        Set<AppUser> users = moduleLookupService.getUsersFromSources(sources);
        log.debug("Found {} USERS from sources", users.size());

        // Собираем данные для уведомлений ДО удаления
        List<UserCleanupData> userData = collectUserCleanupData(users, wordSiteIds, siteId);

        // Удаляем связанные сущности
        deletePart(siteId, siteName, sources);

        // Отправляем уведомления
        sendNotificationPart(userData, siteName);
    }

    // ---------- DELETE BLOCK ----------

    private int deleteNegativeTitleLinks(Long sourceSiteId, String siteName) {
        try {
            return jdbc.update("""
                DELETE FROM user_filter_negative_titles
                WHERE negative_titles_id IN (
                    SELECT id FROM title_word WHERE source_site_id = ?
                )
            """, sourceSiteId);
        } catch (Exception e) {
            log.error("Failed to delete user_filter_negative_titles for SITE={}", siteName, e);
            return 0;
        }
    }

    private int deleteUserFilterTitles(Long sourceSiteId, String siteName) {
        try {
            return jdbc.update("""
                DELETE FROM user_filter_titles
                WHERE titles_id IN (
                    SELECT id FROM title_word WHERE source_site_id = ?
                )
            """, sourceSiteId);
        } catch (Exception e) {
            log.error("Failed to delete user_filter_titles for SITE={}", siteName, e);
            return 0;
        }
    }

    private int deleteTitleWorld(Long sourceSiteId, String siteName) {
        try {
            return jdbc.update("""
                DELETE FROM title_word
                WHERE source_site_id = ?
            """, sourceSiteId);
        } catch (Exception e) {
            log.error("Failed to delete title_word for SITE={}", siteName, e);
            return 0;
        }
    }

    private int deleteOrderModuleSources(Long sourceSiteId, String siteName) {
        try {
            return jdbc.update("""
                DELETE FROM order_modules_sources
                WHERE sources_id = ?
            """, sourceSiteId);
        } catch (Exception e) {
            log.error("Failed to delete order_modules_sources for SITE={}", siteName, e);
            return 0;
        }
    }

    private int deleteSourceSite(Long siteId, String siteName) {
        try {
            return jdbc.update("""
                DELETE FROM source_site
                WHERE site_source = ?
            """, siteId);
        } catch (Exception e) {
            log.error("Failed to delete source_site for siteId={}", siteName, e);
            return 0;
        }
    }

    private void deletePart(Long siteId, String siteName, List<SourceSite> sources) {

        int deletedNegatives = 0;
        int deletedUserFilterTitles = 0;
        int deletedTitleWords = 0;
        int deletedLinks = 0;

        for (SourceSite source : sources) {
            deletedNegatives        += deleteNegativeTitleLinks(source.getId(), siteName);
            deletedUserFilterTitles += deleteUserFilterTitles(source.getId(), siteName);
            deletedTitleWords       += deleteTitleWorld(source.getId(), siteName);
            deletedLinks            += deleteOrderModuleSources(source.getId(), siteName);
        }

        int deletedSites = deleteSourceSite(siteId, siteName);

        log.info("""
→ [CORE_DELETE_STATS]
NEGATIVE_LINKS   = %d
POSITIVE_LINKS   = %d
TITLE_WORDS      = %d
MODULE_SOURCES   = %d
SOURCE_SITES     = %d
""".formatted(
                deletedNegatives,
                deletedUserFilterTitles,
                deletedTitleWords,
                deletedLinks,
                deletedSites
        ));
    }

    // ---------- СБОР ДАННЫХ ПО МОДУЛЯМ ----------

    private List<UserCleanupData> collectUserCleanupData(Set<AppUser> users, List<Long> deletedWordIds, Long siteId) {

        Map<Long, String> wordsMap = wordCleanupRepositoryService.getWordsByIds(deletedWordIds);
        List<UserCleanupData> result = new ArrayList<>();

        for (AppUser user : users) {

            // модули определяются по ПОЛЬЗОВАТЕЛЮ + САЙТУ
            List<OrderModules> modules =
                    orderModulesRepository.findAllByUserAndSite(user.getId(), siteId);

            List<UserModuleCleanupData> moduleDataList = new ArrayList<>();

            for (OrderModules module : modules) {

                List<Long> posIds = wordCleanupRepositoryService.getPositiveWordsForModule(module.getId(), deletedWordIds);
                List<Long> negIds = wordCleanupRepositoryService.getNegativeWordsForModule(module.getId(), deletedWordIds);

                List<String> posWords = posIds.stream()
                        .map(wordsMap::get)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();

                List<String> negWords = negIds.stream()
                        .map(wordsMap::get)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();

                // добавляем модуль
                moduleDataList.add(
                        new UserModuleCleanupData(
                                module.getName(),
                                posWords,
                                negWords
                        )
                );
            }

            result.add(new UserCleanupData(user, moduleDataList));
        }

        return result;
    }

    // ---------- ОТПРАВКА ПИСЕМ ----------
    private void sendNotificationPart(List<UserCleanupData> data, String siteName) {
        for (UserCleanupData d : data) {
            sendNotificationToSingleUser(d.getUser(), siteName, d.getModules());
        }
    }

    private void sendNotificationToSingleUser(
            AppUser user,
            String siteName,
            List<UserModuleCleanupData> modules
    ) {

        boolean wordsDeleted = modules.stream().anyMatch(m ->
                !m.getPositiveWords().isEmpty() || !m.getNegativeWords().isEmpty()
        );

        String htmlMessage = buildCleanupHtmlMessage(siteName, modules, wordsDeleted);

        log.warn("""
→ [CORE_MAIL_PREVIEW]
USER_ID      = %d
EMAIL        = %s
UUID         = %s
SITE         = %s

MESSAGE (HTML):
%s
""".formatted(
                user.getId(),
                user.getEmail(),
                user.getUuid(),
                siteName,
                htmlMessage
        ));

        mailSenderService.sendMessagesToUser(
                user,
                List.of(htmlMessage),
                NotificationType.CLEANUP
        );
    }


    private String buildCleanupHtmlMessage(
            String siteName,
            List<UserModuleCleanupData> modules,
            boolean wordsDeleted
    ) {

        StringBuilder sb = new StringBuilder();
        sb.append("<ul>");

        for (UserModuleCleanupData m : modules) {

            sb.append("<li>");
            sb.append("<b>").append(m.getModuleName()).append("</b>");

            if (!m.getPositiveWords().isEmpty() || !m.getNegativeWords().isEmpty()) {
                sb.append("<br>");

                if (!m.getPositiveWords().isEmpty()) {
                    sb.append("Позитивные: <b>")
                            .append(String.join(", ", m.getPositiveWords()))
                            .append("</b><br>");
                }

                if (!m.getNegativeWords().isEmpty()) {
                    sb.append("Негативные: <b>")
                            .append(String.join(", ", m.getNegativeWords()))
                            .append("</b><br>");
                }
            }

            sb.append("</li>");
        }

        sb.append("</ul>");

        String modulesBlock = sb.toString();

        if (wordsDeleted) {
            return """
            <p>Уважаемый пользователь!</p>
            <p>Мы обновили категории сайта <b>%s</b>.</p>

            <p>Некоторые категории были удалены или изменены.<br>
            Были удалены следующие ключевые слова из Ваших модулей:</p>

            %s

            <p>Вам необходимо заново настроить фильтры поиска Ваших заказов.</p>
            """.formatted(siteName, modulesBlock);
        } else {
            return """
            <p>Уважаемый пользователь!</p>
            <p>Мы обновили категории сайта <b>%s</b>.</p>

            <p>Некоторые категории были удалены или изменены.<br>
            Ваши модули были затронуты, так как категории были обновлены:</p>

            %s

            <p>Вам необходимо заново настроить фильтры поиска Ваших заказов.</p>
            """.formatted(siteName, modulesBlock);
        }
    }



}
