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

    public void cleanupParserSourceForSite(Long siteId, String siteName) {
        //Получаем все настроенные источники для сайта, категории которого нужно удалить
        List<SourceSite> sources = sourceSiteRepository.findAllBySiteSource(siteId);
        log.debug("Found {} sources", sources.size());
        log.debug("Starting CORE cleanup for siteId={}", siteId);
        //Получаем всех пользователей из источников, которым необходимо отправить уведомления
        Set<AppUser> users = getUsersFromSources(sources);
        //Удаляем связанные сущности
        deletePart(siteId, sources);
        //Отправляем уведомления
        sendNotificationPart(users, siteName);
        log.debug("CORE cleanup completed for siteId={}", siteId);
    }

    @Transactional
    private int deleteTitleWorld(Long sourceSiteId){
        // 1. Удаляем title_word
        return jdbc.update("""
            DELETE FROM title_word
            WHERE source_site_id = ?
        """, sourceSiteId);
    }

    @Transactional
    private int deleteOrderModuleSources(Long sourceSiteId){
        // 2. Удаляем связи в order_modules_sources
        return jdbc.update("""
            DELETE FROM order_modules_sources
            WHERE sources_id = ?
        """, sourceSiteId);
    }

    @Transactional
    private int deleteSourceSite(Long siteId){
        return jdbc.update("""
            DELETE FROM source_site
            WHERE site_source = ?
        """, siteId);
    }

    private void deletePart(Long siteId, List<SourceSite> sources){
        int deletedTitleWords = 0;
        for (SourceSite source : sources) {
            deletedTitleWords += deleteTitleWorld(source.getId());
        }
        log.debug("Deleted {} title_word rows", deletedTitleWords);

        // 2. Удаляем связи в order_modules_sources
        int deletedLinks = 0;
        for (SourceSite source : sources) {
            deletedLinks += deleteOrderModuleSources(source.getId());
        }
        log.debug("Deleted {} order_modules_sources rows", deletedLinks);

        // 3. Удаляем сам SourceSite
        int deletedSites = deleteSourceSite(siteId);
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
        log.debug("Sending notifications for site={} to {} users", siteName, users.size());

        String msg = String.format(CATEGORY_REMOVE_MESSAGE, siteName);
        log.debug("Notification message: {}", msg);

        users.forEach(user -> {
            log.debug("Sending notification to user id={} email={} uuid={}",
                    user.getId(),
                    user.getEmail(),
                    user.getUuid());
            mailSenderService.sendMessagesToUser(user, List.of(msg));
        });
        log.debug("Finished sending notifications for site={}", siteName);
    }


}

