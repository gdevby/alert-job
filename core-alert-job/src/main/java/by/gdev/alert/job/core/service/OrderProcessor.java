package by.gdev.alert.job.core.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import by.gdev.alert.job.core.client.LlmClient;
import by.gdev.alert.job.core.client.NotificationClient;
import by.gdev.alert.job.core.model.ai.AiOrderRequest;
import by.gdev.alert.job.core.model.db.*;
import by.gdev.alert.job.core.model.db.ai.AccountTemplateBinding;
import by.gdev.alert.job.core.model.db.ai.UserSiteCredential;
import by.gdev.alert.job.core.repository.ai.AccountTemplateBindingRepository;
import by.gdev.alert.job.core.repository.ai.UserSiteCredentialRepository;
import by.gdev.alert.job.core.service.ai.AiOrderRequestMapper;
import by.gdev.common.model.NotificationType;
import by.gdev.common.model.SiteName;
import lombok.Getter;
import org.apache.commons.lang.StringUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import by.gdev.alert.job.core.configuration.ApplicationProperty;
import by.gdev.alert.job.core.repository.AppUserRepository;
import by.gdev.alert.job.core.repository.DelayOrderNotificationRepository;
import by.gdev.alert.job.core.repository.UserFilterRepository;
import by.gdev.common.model.OrderDTO;
import by.gdev.common.model.SourceSiteDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderProcessor {
    private final StatisticService statisticService;
    private final AppUserRepository userRepository;
    private final DelayOrderNotificationRepository delayOrderRepository;
    private final ApplicationProperty property;
    private final UserFilterRepository filterRepository;
    private final MailSenderService mailSenderService;
    private final WebhookSenderService webhookSenderService;

    private final AccountTemplateBindingRepository accountTemplateBindingRepository;
    private final UserSiteCredentialRepository userSiteCredentialRepository;
    private final LlmClient llmClient;
    private final NotificationClient notificationClient;
    private final AiOrderRequestMapper aiOrderRequestMapper;

    @Value("${autoreply.enabled:false}")
    @Getter
    private boolean autoReplyEnabled;

    // Создаём кеш
    private final Cache<String, Boolean> sentLinks = Caffeine.newBuilder()
            .expireAfterWrite(24, TimeUnit.HOURS) // живёт 24 часа
            .maximumSize(50000)                    // максимум 50 000 записей
            .build();

    public void forEachOrders(Set<AppUser> users, List<OrderDTO> orders) {
        for (OrderDTO order : orders) {
            try {
                statisticService.statisticTitleWord(order.getTitle(), order.getSourceSite());
            } catch (Exception e) {
                log.warn("Ошибка при статистике для заказа {}: {}", order.getLink(), e.getMessage());
            }
        }

        Map<Long, UserFilter> map = filterRepository.findByIdEagerAllWordsAll().stream()
                .collect(Collectors.toMap(e -> e.getId(), Function.identity()));

        users.stream()
                .filter(AppUser::isSwitchOffAlerts)
                .forEach(user -> {
                    user.getOrderModules().stream()
                            .filter(orderModule -> Objects.nonNull(orderModule.getCurrentFilter()))
                            .forEach(orderModule -> {
                                UserFilter currentFilter = map.get(orderModule.getCurrentFilter().getId());
                                if (currentFilter == null) return;

                                orderModule.getSources().forEach(sourceSite -> {
                                    List<OrderDTO> matchedOrders = orders.parallelStream()
                                            .filter(order -> compareSiteSources(order.getSourceSite(), sourceSite))
                                            .filter(order -> isMatchUserFilter(order, currentFilter))
                                            .collect(Collectors.toList());

                                    if (matchedOrders.isEmpty()) return;

                                    matchedOrders.forEach(order -> order.setModuleName(orderModule.getName()));
                                    List<OrderDTO> filtered = matchedOrders.stream()
                                            .filter(order -> {
                                                String key = user.getUuid() + "|" + order.getLink();
                                                if (sentLinks.getIfPresent(key) != null) {
                                                    log.info("Дубликат заказа {} для пользователя {}, пропускаем", key, user.getEmail());
                                                    return false;
                                                }
                                                sentLinks.put(key, true);
                                                return true;
                                            })
                                            .toList();

                                    if (!filtered.isEmpty()) {
                                        sendOrderToUser(user, filtered);
                                    }

                                    if (!autoReplyEnabled || !Boolean.TRUE.equals(orderModule.getAutoReplyEnabled())) {
                                        return;
                                    }

                                    SiteName siteName = SiteName.fromId(sourceSite.getSiteSource());
                                    if (!notificationClient.canParse(siteName.name())) {
                                        log.warn("АВТООТВЕТ: пользователь {} -> сайт {} НЕ ПОДДЕРЖИВАЕТСЯ",
                                                user.getEmail(), siteName);
                                        return;
                                    }

                                    try {
                                        buildAndsSndLlmRequest(user, orderModule, sourceSite, matchedOrders);
                                    } catch (Exception e) {
                                        log.error("АВТООТВЕТ: пользователь {} -> ОШИБКА в модуле {}: {}",
                                                user.getEmail(), orderModule.getName(), e.getMessage(), e);
                                    }
                                });
                            });
                });
    }

    private void buildAndsSndLlmRequest(AppUser user, OrderModules orderModule, SourceSite sourceSite, List<OrderDTO> orders) {
        if (orders.isEmpty()) {
            log.warn("АВТООТВЕТ: {} -> список заказов пуст, пропускаем", orderModule.getName());
            return;
        }

        Long siteId = sourceSite.getSiteSource();

        // Получаем все креды пользователя для этого сайта
        List<UserSiteCredential> credentials = userSiteCredentialRepository.findByUserUuidAndSiteId(user.getUuid(), siteId);
        if (credentials.isEmpty()) {
            return;
        }

        // Ищем кред, для которого есть активный биндинг с данным модулем
        UserSiteCredential selectedCredential = null;
        AccountTemplateBinding selectedBinding = null;

        for (UserSiteCredential cred : credentials) {
            Optional<AccountTemplateBinding> optBinding = accountTemplateBindingRepository
                    .findByModuleIdAndAccountIdAndActiveTrue(orderModule.getId(), cred.getId());
            if (optBinding.isPresent()) {
                selectedCredential = cred;
                selectedBinding = optBinding.get();
                break;
            }
        }

        if (selectedBinding == null) {
            return;
        }

        log.info("АВТООТВЕТ: {} -> НАЧАЛО ОБРАБОТКИ: пользователь={}, сайт={}, заказов={}",
                orderModule.getName(), user.getEmail(), siteId, orders.size());
        log.info("АВТООТВЕТ: {} -> найдено аккаунтов для пользователя: {}", orderModule.getName(), credentials.size());
        log.info("АВТООТВЕТ: {} -> найден активный биндинг для аккаунта id={}, templateId={}, promtId={}",
                orderModule.getName(), selectedCredential.getId(), selectedBinding.getTemplateId(), selectedBinding.getPromtId());

        Long credentialId = selectedCredential.getId();
        Long templateId = selectedBinding.getTemplateId();
        Long promtId = selectedBinding.getPromtId();

        log.info("АВТООТВЕТ: {} -> ОТПРАВКА ЗАПРОСА В LLM: credentialId={}, templateId={}, promtId={}, заказов={}",
                orderModule.getName(), credentialId, templateId, promtId, orders.size());

        // Формируем и отправляем запрос
        AiOrderRequest aiOrderRequest = aiOrderRequestMapper.build(user, orderModule,
                credentialId, templateId, promtId, orders, selectedBinding.getNotificationType());
        log.info("АВТООТВЕТ: {} -> запрос сформирован, отправка в llmClient", orderModule.getName());
        llmClient.sendAiOrderRequest(aiOrderRequest);
    }

    private void sendOrderToUser(AppUser user, List<OrderDTO> list) {
        // Webhook — канал для машин: он не ждёт окна оповещений и не заменяет
        // почту с телеграмом, а работает рядом с ними.
        webhookSenderService.sendOrders(user, list);

        if (CollectionUtils.isEmpty(user.getUserAlertTimes()) || isMatchUserAlertTimes(user)) {
            Map<String, List<OrderDTO>> byLink = list.stream().collect(Collectors.groupingBy(OrderDTO::getLink));
            List<String> resultOrdersString = byLink.entrySet().stream().map(entry -> {
                List<OrderDTO> orderList = entry.getValue();

                String modulesString = orderList.stream().map(order -> {
                    return order.getModuleName();
                }).distinct().collect(Collectors.joining(", "));
                SourceSiteDTO s = orderList.get(0).getSourceSite();
                return user.isDefaultSendType() ? createOrdersMessageEmail(modulesString, orderList.get(0).getTitle(), orderList.get(0).getLink(), s.getCategoryName(),
                        s.getSubCategoryName()) : createOrdersMessageTelegram(modulesString, orderList.get(0).getTitle(), orderList.get(0).getLink(), s.getCategoryName(),
                        s.getSubCategoryName());
            }).toList();
            mailSenderService.sendMessagesToUser(user, resultOrdersString, NotificationType.ORDER);
        } else {
            list.forEach(l -> {
                SourceSiteDTO s = l.getSourceSite();
                DelayOrderNotification don = new DelayOrderNotification();
                don.setUser(user);
                don.setLink(l.getLink());
                don.setTitle(l.getTitle());
                don.setOrderName(l.getModuleName());
                don.setCategoryName(s.getCategoryName());
                if (!StringUtils.isEmpty(s.getSubCategoryName())) {
                    don.setSubCategoryName(s.getSubCategoryName());
                }
                delayOrderRepository.save(don);
            });
        }
    }

    private boolean compareSiteSources(SourceSiteDTO orderSource, SourceSite userSource) {
        return userSource.getSiteSource().equals(orderSource.getSource())
                && userSource.getSiteCategory().equals(orderSource.getCategory())
                && Objects.equals(userSource.getSiteSubCategory(), orderSource.getSubCategory());
    }

    public boolean isMatchUserFilter(OrderDTO order, UserFilter userFilter) {
        boolean minValue = true, maxValue = true, containsTitle = false, containsTech = false, containsDesc = false,
                containsTitle1 = false, containsTech1 = false, containsDesc1 = false;
        if (property.getSitesOpenForAll().contains(order.getSourceSite().getSource()) && userFilter.isOpenForAll()) {
            boolean openForAll = order.isOpenForAll() == userFilter.isOpenForAll();
            if (openForAll == false) {
                return false;
            }
        }
        if (Objects.nonNull(order.getPrice())) {
            if (Objects.nonNull(userFilter.getMinValue())) {
                minValue = userFilter.getMinValue() <= order.getPrice().getValue();
            }
            if (Objects.nonNull(userFilter.getMaxValue()) && userFilter.getMaxValue() != 0) {
                maxValue = userFilter.getMaxValue() >= order.getPrice().getValue();
            }
            if ((!minValue || !maxValue) && !CollectionUtils.isEmpty(userFilter.getDescriptionWordPrice())) {
                boolean dwp = userFilter.getDescriptionWordPrice().stream()
                        .anyMatch(e -> StringUtils.containsIgnoreCase(order.getMessage(), e.getName()));
                if (dwp) {
                    maxValue = true;
                    minValue = true;
                }
            }
            if (!(minValue && maxValue)) {
                log.trace("ignore by min max {} {} {}", minValue, maxValue, order.getLink());
                return false;
            }
        }
        if (CollectionUtils.isEmpty(userFilter.getTitles()) && CollectionUtils.isEmpty(userFilter.getDescriptions())) {
            return true;
        }
        if (!CollectionUtils.isEmpty(userFilter.getTitles())) {
            containsTitle = userFilter.getTitles().stream()
                    .anyMatch(e -> StringUtils.containsIgnoreCase(order.getTitle(), e.getName()));
        }
        if (!CollectionUtils.isEmpty(userFilter.getDescriptions())) {
            containsDesc = userFilter.getDescriptions().stream()
                    .anyMatch(e -> StringUtils.containsIgnoreCase(order.getMessage(), e.getName()));
        }
        if (containsTitle || containsDesc || containsTech) {
            log.trace("some positive is true title {} desc {} tech {} {}", containsTitle, containsDesc, containsTech,
                    order.getLink());
            if (userFilter.isActivatedNegativeFilters()) {
                if (!CollectionUtils.isEmpty(userFilter.getNegativeDescriptions())) {
                    containsDesc1 = userFilter.getNegativeDescriptions().stream()
                            .anyMatch(e -> StringUtils.containsIgnoreCase(order.getMessage(), e.getName()));
                }

                if (!CollectionUtils.isEmpty(userFilter.getNegativeTitles())) {
                    containsTitle1 = userFilter.getNegativeTitles().stream()
                            .anyMatch(e -> StringUtils.containsIgnoreCase(order.getTitle(), e.getName()));
                }
                log.trace("negative filter title {} desc {} tech {} {}", containsTitle1, containsDesc1, containsTech1,
                        order.getLink());
                return !(containsDesc1 || containsTech1 || containsTitle1);
            } else {
                return true;
            }
        }
        return false;
    }

    @Scheduled(cron = "0 0/10 * * * *")
    public void sendDelayOrders() {
        userRepository.findAllOneEagerUserAlertTimes().forEach(user -> {
            boolean isMatchAlertdate = isMatchUserAlertTimes(user);
            if (isMatchAlertdate) {
                Map<String, List<DelayOrderNotification>> byLink = user.getDelayOrderNotifications().stream().collect(Collectors.groupingBy(DelayOrderNotification::getLink));
                List<String> resultOrdersString = byLink.entrySet().stream().map(entry -> {
                    List<DelayOrderNotification> orderList = entry.getValue();

                    String modulesString = orderList.stream().map(DelayOrderNotification::getOrderName)
                            .distinct()
                            .collect(Collectors.joining(", "));
                    return user.isDefaultSendType() ? createOrdersMessageEmail(modulesString, orderList.get(0).getTitle(), orderList.get(0).getLink(), orderList.get(0).getCategoryName(),
                            orderList.get(0).getSubCategoryName()) : createOrdersMessageTelegram(modulesString, orderList.get(0).getTitle(), orderList.get(0).getLink(), orderList.get(0).getCategoryName(),
                            orderList.get(0).getSubCategoryName());
                }).toList();
                if (!resultOrdersString.isEmpty()) {
                    //sendMessageToUser(user, resultOrdersString);
                    mailSenderService.sendMessagesToUser(user, resultOrdersString ,NotificationType.ORDER);
                    delayOrderRepository.deleteAll(user.getDelayOrderNotifications());
                }
            }
        });
    }

    private boolean isMatchUserAlertTimes(AppUser user) {
        return user.getUserAlertTimes().stream().anyMatch(pr -> {
            LocalDateTime time = LocalDateTime.now(ZoneId.of(pr.getTimeZone()));
            Integer day = time.getDayOfWeek().getValue();
            Integer hour = time.getHour();
            return pr.getAlertDate().equals(day) && pr.getStartAlert() <= hour && hour < pr.getEndAlert();
        });
    }

    private String createOrdersMessageTelegram(String name, String title, String link, String categoryName,
                                       String subCategoryName) {
        return StringUtils.isNotEmpty(subCategoryName)
                ? String.format("%s, %s, %s новый заказ - %s %s", name, categoryName, subCategoryName, title, link)
                : String.format("%s, %s, новый заказ - %s %s", name, categoryName, title, link);
    }

    private String createOrdersMessageEmail(String moduleName,
                                       String title,
                                       String link,
                                       String categoryName,
                                       String subCategoryName) {

        String subCategoryBlock = "";
        if (StringUtils.isNotEmpty(subCategoryName)) {
            subCategoryBlock = String.format(
                    "<p style=\"margin: 4px 0;\"><strong>Подкатегория:</strong> %s</p>",
                    subCategoryName
            );
        }

        return String.format("""
            <div style="font-family: Arial, sans-serif; padding: 12px; border: 1px solid #e5e5e5; border-radius: 8px; background: #fafafa; margin-bottom: 12px;">
                <h3 style="margin: 0 0 10px 0; color: #333;">Новый заказ</h3>

                <p style="margin: 4px 0;">
                    <strong>Модуль:</strong> %s
                </p>

                <p style="margin: 4px 0;">
                    <strong>Категория:</strong> %s
                </p>

                %s

                <p style="margin: 4px 0;">
                    <strong>Название:</strong> %s
                </p>

                <p style="margin: 4px 0;">
                    <strong>Ссылка:</strong>
                    <a href="%s" style="color: #1a73e8;">%s</a>
                </p>
            </div>
            """,
                moduleName,
                categoryName,
                subCategoryBlock,
                title,
                link,
                link
        );
    }
}