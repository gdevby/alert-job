    package by.gdev.alert.job.core.service.cleanup;

    import by.gdev.alert.job.core.model.cleanup.ParserCategoryDTO;
    import by.gdev.alert.job.core.model.db.AppUser;
    import by.gdev.alert.job.core.model.db.OrderModules;
    import by.gdev.alert.job.core.model.db.SourceSite;
    import by.gdev.alert.job.core.repository.OrderModulesRepository;
    import by.gdev.alert.job.core.repository.SourceSiteRepository;
    import by.gdev.alert.job.core.service.MailSenderService;
    import by.gdev.alert.job.core.service.cleanup.components.ModuleLookupService;
    import by.gdev.alert.job.core.service.cleanup.components.WordCleanupRepositoryService;
    import by.gdev.alert.job.core.templates.MessageTemplates;
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
        public void cleanupParserSourceForSite(Long siteId, String siteName, List<ParserCategoryDTO> categories)
        {
            List<SourceSite> sources = sourceSiteRepository.findAllBySiteSource(siteId);
            log.debug("Found {} SOURCES", sources.size());

            List<Long> wordSiteIds = sources.stream()
                    .flatMap(s -> wordCleanupRepositoryService.getTitleWordIds(s.getId()).stream())
                    .distinct()
                    .toList();

            log.debug("Word size {}", wordSiteIds.size());

            Set<AppUser> users = moduleLookupService.getUsersFromSources(sources);

            Map<Long, ParserCategoryDTO> categoryMap = buildCategoryMap(categories);

            log.debug("Found {} USERS from sources", users.size());

            // Собираем данные для уведомлений ДО удаления
            List<UserCleanupData> userData = collectUserCleanupData(users, wordSiteIds, siteId, sources, categoryMap);

            // Удаляем связанные сущности
            deletePart(siteId, siteName, sources);

            // Отправляем уведомления
            sendNotificationPart(userData, siteName);
        }

        private Map<Long, ParserCategoryDTO> buildCategoryMap(List<ParserCategoryDTO> categories) {
            Map<Long, ParserCategoryDTO> map = new HashMap<>();

            for (ParserCategoryDTO dto : categories) {
                if (dto.subCategoryId() != null) {
                    map.put(dto.subCategoryId(), dto); // ключ — подкатегория
                } else {
                    map.put(dto.categoryId(), dto); // если нет подкатегории
                }
            }

            return map;
        }

        private List<ParserCategoryDTO> getCategoriesForModule(
                OrderModules module,
                List<SourceSite> allSources,
                Map<Long, ParserCategoryDTO> categoryMap
        ) {
            List<Long> sourceIds = moduleLookupService.getSourceIdsForModule(module.getId());

            List<ParserCategoryDTO> result = new ArrayList<>();

            for (SourceSite source : allSources) {
                if (sourceIds.contains(source.getId())) {
                    ParserCategoryDTO dto = findCategoryForSource(source, categoryMap);
                    if (dto != null) {
                        result.add(dto);
                    }
                }
            }

            return result;
        }

        private ParserCategoryDTO findCategoryForSource(SourceSite source, Map<Long, ParserCategoryDTO> map) {

            // если есть подкатегория — она приоритетная
            if (source.getSiteSubCategory() != null) {
                ParserCategoryDTO dto = map.get(source.getSiteSubCategory());
                if (dto != null) return dto;
            }

            // иначе ищем по категории
            return map.get(source.getSiteCategory());
        }

        private List<ParserCategoryDTO> getUserCategories(
                AppUser user,
                Long siteId,
                List<SourceSite> allSources,
                Map<Long, ParserCategoryDTO> categoryMap
        ) {
            List<OrderModules> modules =
                    orderModulesRepository.findAllByUserAndSite(user.getId(), siteId);

            Set<Long> userSourceIds = new HashSet<>();

            for (OrderModules module : modules) {
                userSourceIds.addAll(moduleLookupService.getSourceIdsForModule(module.getId()));
            }

            List<ParserCategoryDTO> result = new ArrayList<>();

            for (SourceSite source : allSources) {
                if (userSourceIds.contains(source.getId())) {
                    ParserCategoryDTO dto = findCategoryForSource(source, categoryMap);
                    if (dto != null) {
                        result.add(dto);
                    }
                }
            }

            return result;
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

        private List<UserCleanupData> collectUserCleanupData(Set<AppUser> users, List<Long> deletedWordIds,
                                                             Long siteId, List<SourceSite> sources, Map<Long, ParserCategoryDTO> categoryMap) {

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

                    //позитивные слова
                    List<String> posWords = posIds.stream()
                            .map(wordsMap::get)
                            .filter(Objects::nonNull)
                            .distinct()
                            .toList();

                    //негативные слова
                    List<String> negWords = negIds.stream()
                            .map(wordsMap::get)
                            .filter(Objects::nonNull)
                            .distinct()
                            .toList();

                    //категории модуля
                    List<ParserCategoryDTO> moduleCategories =
                            getCategoriesForModule(module, sources, categoryMap);

                    // добавляем модуль
                    moduleDataList.add(
                            new UserModuleCleanupData(
                                    module.getName(),
                                    posWords,
                                    negWords,
                                    moduleCategories
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

            String finalMessage = MessageTemplates.Cleanup.buildCleanupHtmlMessage(siteName, modules, wordsDeleted);

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
                    finalMessage
            ));

            mailSenderService.sendRequiredMessagesToUser(
                    user,
                    List.of(finalMessage),
                    NotificationType.CLEANUP
            );
        }
    }
