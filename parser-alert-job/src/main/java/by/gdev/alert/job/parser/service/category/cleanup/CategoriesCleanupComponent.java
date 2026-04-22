package by.gdev.alert.job.parser.service.category.cleanup;

import by.gdev.alert.job.parser.domain.CleanupRequest;
import by.gdev.alert.job.parser.domain.ParserCategoryDTO;
import by.gdev.alert.job.parser.domain.db.Category;
import by.gdev.alert.job.parser.domain.db.SiteSourceJob;
import by.gdev.alert.job.parser.domain.db.Subcategory;
import by.gdev.alert.job.parser.repository.SiteSourceJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CategoriesCleanupComponent {

    private final JdbcTemplate jdbc;

    private final SiteSourceJobRepository siteSourceJobRepository;

    @Value("#{'${cleanup.sites:}'.split(',')}")
    private List<String> sites;

    @Value("${core.module.url}")
    private String coreModuleUrl;

    @Value("${cleanup.api.url}")
    private String cleanupApiUrl;

    //@PostConstruct
    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        log.info("=== CLEANUP ON STARTUP ===");

        // если проперти нет или список пустой — выходим
        if (sites == null || sites.isEmpty() || (sites.size() == 1 && sites.get(0).isBlank())) {
            log.warn("No cleanup sites configured. Skipping cleanup.");
            return;
        }

        cleanup();
    }

    public void cleanup() {
        for (String site : sites) {
            site = site.trim();
            if (site.isBlank()) {
                log.warn("Skipping empty site entry in cleanup.sites");
                continue;
            }
            cleanup(site);
        }
    }

    @Transactional
    public void cleanup(String siteName) {
        log.debug("=== CLEANUP {} STARTED ===", siteName);
        List<ParserCategoryDTO> categories = loadCategoriesForSite(siteName);
        deletePart(siteName);
        cleanupCoreModule(siteName, categories);
        log.debug("=== CLEANUP {} FINISHED ===", siteName);
    }

    public List<ParserCategoryDTO> loadCategoriesForSite(String siteName) {
        SiteSourceJob job = siteSourceJobRepository.findWithCategories(siteName);
        if (job == null) {
            throw new IllegalArgumentException("Site not found: " + siteName);
        }

        List<ParserCategoryDTO> result = new ArrayList<>();

        for (Category c : job.getCategories()) {

            // Если у категории нет подкатегорий — всё равно отправляем
            if (c.getSubCategories() == null || c.getSubCategories().isEmpty()) {
                result.add(new ParserCategoryDTO(
                        c.getId(),
                        c.getNativeLocName(),
                        null,
                        null
                ));
                continue;
            }

            // Иначе — отправляем каждую подкатегорию
            for (Subcategory sc : c.getSubCategories()) {
                result.add(new ParserCategoryDTO(
                        c.getId(),
                        c.getNativeLocName(),
                        sc.getId(),
                        sc.getNativeLocName()
                ));
            }
        }
        return result;
    }

    @Transactional
    private void cleanupOrderLinks(String siteName) {
        int count = jdbc.update("""
            DELETE ol FROM order_links ol
            JOIN parser_category c ON ol.category_id = c.id
            JOIN site_source_job ssj ON c.site_source_job_id = ssj.id
            WHERE ssj.name = ?;
        """, siteName);
        log.info("[{}] Deleted order_links: {}", siteName, count);
    }

    @Transactional
    private void cleanupSubcategories(String siteName) {
        int count = jdbc.update("""
            DELETE sc FROM parser_sub_category sc
            JOIN parser_category c ON sc.category_id = c.id
            JOIN site_source_job ssj ON c.site_source_job_id = ssj.id
            WHERE ssj.name = ?;
        """, siteName);
        log.info("[{}] Deleted subcategories: {}", siteName, count);
    }

    @Transactional
    private void cleanupCategories(String siteName) {
        int count = jdbc.update("""
            DELETE c FROM parser_category c
            JOIN site_source_job ssj ON c.site_source_job_id = ssj.id
            WHERE ssj.name = ?;
        """, siteName);
        log.info("[{}] Deleted categories: {}", siteName, count);
    }

    @Transactional
    private void cleanupParserOrders(String siteName) {
        int count = jdbc.update("""
        DELETE o FROM parser_order o
        JOIN parser_order_source ps ON o.source_site_id = ps.id
        JOIN site_source_job ssj ON ssj.id = ps.source
        WHERE ssj.name = ?;
    """, siteName);
        log.info("[{}] Deleted parser_order: {}", siteName, count);
    }

    @Transactional
    private void cleanupParserSources(String siteName) {
        int count = jdbc.update("""
        DELETE ps FROM parser_order_source ps
        JOIN site_source_job ssj ON ssj.id = ps.source
        WHERE ssj.name = ?;
    """, siteName);
        log.info("[{}] Deleted parser_order_source: {}", siteName, count);
    }

    private void cleanupCoreModule(String siteName, List<ParserCategoryDTO> categories) {
        try {
            SiteSourceJob job = siteSourceJobRepository.findByName(siteName);
            if (job == null) {
                throw new IllegalArgumentException("Site not found: " + siteName);
            }
            Long siteId = job.getId();

            String url = coreModuleUrl + cleanupApiUrl;

            CleanupRequest body = new CleanupRequest(
                    siteId,
                    siteName,
                    categories
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<CleanupRequest> entity = new HttpEntity<>(body, headers);

            RestTemplate rest = new RestTemplate();

            log.debug("[{}] Calling CORE cleanup: {}", siteName, url);

            rest.postForEntity(url, entity, String.class);

            log.debug("[{}] CORE cleanup completed", siteName);

        } catch (Exception e) {
            log.error("[{}] CORE cleanup FAILED: {}", siteName, e.getMessage(), e);
        }
    }

    private void deletePart(String siteName){
        cleanupOrderLinks(siteName);
        cleanupSubcategories(siteName);
        cleanupCategories(siteName);
        cleanupParserOrders(siteName);
        cleanupParserSources(siteName);
    }

}

