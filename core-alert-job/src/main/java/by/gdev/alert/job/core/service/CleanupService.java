package by.gdev.alert.job.core.service;

import by.gdev.alert.job.core.model.db.AppUser;
import by.gdev.alert.job.core.model.db.OrderModules;
import by.gdev.alert.job.core.model.db.SourceSite;
import by.gdev.alert.job.core.repository.OrderModulesRepository;
import by.gdev.alert.job.core.repository.SourceSiteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;


@RequiredArgsConstructor
@Component
@Slf4j
public class CleanupService {
    private final SourceSiteRepository sourceSiteRepository;
    private final OrderModulesRepository orderModulesRepository;
    private final MailSenderService mailSenderService;
    private final JdbcTemplate jdbc;

    private static final String CATEGORY_REMOVE_MESSAGE =
            "Уважаемый пользователь! Мы обновили категории сайта %s. Вам необходимо заново настроить фильтры поиска Ваших заказов";

    @Transactional
    public void cleanupParserSourceForSite(Long siteId, String siteName) {
        //Получаем все настроенные источники для сайта, категории которого нужно удалить
        List<SourceSite> sources = sourceSiteRepository.findAllBySiteSource(siteId);
        log.debug("Found {} SOURCES", sources.size());
        log.debug("Starting CORE cleanup for siteId={}", siteId);
        //Получаем всех пользователей из источников, которым необходимо отправить уведомления
        Set<AppUser> users = getUsersFromSources(sources);
        log.debug("Found {} USERS from sources", users.size());
        //Удаляем связанные сущности
        deletePart(siteId, siteName, sources);
        //Отправляем уведомления
        sendNotificationPart(users, siteName);
        log.debug("CORE cleanup completed for siteId={}", siteId);
    }

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

    @Transactional
    private void deletePart(Long siteId, String siteName, List<SourceSite> sources){

        int deletedNegatives = 0;
        for (SourceSite source : sources) {
            deletedNegatives += deleteNegativeTitleLinks(source.getId(), siteName);
        }
        log.debug("Deleted {} user_filter_negative_titles rows", deletedNegatives);

        int deletedUserFilterTitles = 0;
        for (SourceSite source : sources) {
            deletedUserFilterTitles += deleteUserFilterTitles(source.getId(), siteName);
        }
        log.debug("Deleted {} user_filter_titles rows", deletedUserFilterTitles);

        int deletedTitleWords = 0;
        for (SourceSite source : sources) {
            deletedTitleWords += deleteTitleWorld(source.getId(), siteName);
        }
        log.debug("Deleted {} title_word rows", deletedTitleWords);

        int deletedLinks = 0;
        for (SourceSite source : sources) {
            deletedLinks += deleteOrderModuleSources(source.getId(), siteName);
        }
        log.debug("Deleted {} order_modules_sources rows", deletedLinks);

        int deletedSites = deleteSourceSite(siteId, siteName);
        log.debug("Deleted {} source_site rows", deletedSites);
    }


    private Set<AppUser> getUsersFromSources(List<SourceSite> sources) {
        return sources.stream()
                .flatMap(source -> orderModulesRepository
                        .findAllBySourceId(source.getId())
                        .stream())
                .map(OrderModules::getUser)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private void sendNotificationPart(Set<AppUser> users, String siteName) {
        log.debug("Sending cleanup notifications for site={} to {} users", siteName, users.size());

        String msg = String.format(CATEGORY_REMOVE_MESSAGE, siteName);
        log.debug("Notification cleanup message: {}", msg);

        users.forEach(user -> {
            log.debug("→ [CORE_MAIL_CLEANUP_NOTIFY] user id={} email={} uuid={}",
                    user.getId(),
                    user.getEmail(),
                    user.getUuid());
            mailSenderService.sendMessagesToUser(user, List.of(msg));
        });
        log.debug("Finished sending notifications for site={}", siteName);
    }


}

