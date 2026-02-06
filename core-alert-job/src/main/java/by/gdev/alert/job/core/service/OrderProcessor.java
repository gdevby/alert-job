package by.gdev.alert.job.core.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClient;

import by.gdev.alert.job.core.configuration.ApplicationProperty;
import by.gdev.alert.job.core.model.db.AppUser;
import by.gdev.alert.job.core.model.db.DelayOrderNotification;
import by.gdev.alert.job.core.model.db.SourceSite;
import by.gdev.alert.job.core.model.db.UserFilter;
import by.gdev.alert.job.core.repository.AppUserRepository;
import by.gdev.alert.job.core.repository.DelayOrderNotificationRepository;
import by.gdev.alert.job.core.repository.UserFilterRepository;
import by.gdev.common.model.OrderDTO;
import by.gdev.common.model.SourceSiteDTO;
import by.gdev.common.model.UserNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderProcessor {
    private final WebClient webClient;
    private final StatisticService statisticService;
    private final AppUserRepository userRepository;
    private final DelayOrderNotificationRepository delayOrderRepository;
    private final ApplicationProperty property;
    private final UserFilterRepository filterRepository;

    @Value("${telegram.max.failures:5}")
    private int maxTelegramFailures;

    private static final String NEW_LINE = "\n";
    private static final String SEND_MESSAGE_URL_TELEGRAM = "http://notification:8019/telegram";
    private static final String SEND_MESSAGE_URL_MAIL = "http://notification:8019/mail";
    private static final String TELEGRAM_WARNING_MESSAGE = "Здравствуйте! Обнаружены проблемы с отправкой уведомлений в Telegram. "
            + "Проверьте, не заблокирован ли бот. Временные уведомления будут приходить на email.";

    public void forEachOrders(Set<AppUser> users, List<OrderDTO> orders) {
        orders.forEach(orderDTO -> statisticService.statisticTitleWord(orderDTO.getTitle(), orderDTO.getSourceSite()));
        Map<Long, UserFilter> map = filterRepository.findByIdEagerAllWordsAll().stream()
                .collect(Collectors.toMap(e -> e.getId(), Function.identity()));
        users.stream().forEach(user -> {
            List<OrderDTO> orderListToSend = user.getOrderModules().stream().filter(orderModule -> Objects.nonNull(orderModule.getCurrentFilter()))
                    .map(orderModule -> {
                        UserFilter currentFilter = map.get(orderModule.getCurrentFilter().getId());
                        return orderModule.getSources().stream().map(s -> {
                            List<OrderDTO> list = orders.parallelStream()
                                    .peek(orderDTO -> {})
                                    .filter(f -> compareSiteSources(f.getSourceSite(), s))
                                    .filter(f -> isMatchUserFilter(f, currentFilter))
                                    .map(order -> {
                                        order.setModuleName(orderModule.getName());
                                        return order;
                                    }).collect(Collectors.toList());
                            return list;
                        }).flatMap(list -> list.stream()).toList();
                    }).flatMap(list -> list.stream()).toList();
            if (!orderListToSend.isEmpty()) {
                sendOrderToUser(user, orderListToSend);
            }
        });
    }

    private void sendOrderToUser(AppUser user, List<OrderDTO> list) {
        if (CollectionUtils.isEmpty(user.getUserAlertTimes()) || isMatchUserAlertTimes(user)) {
            Map<String, List<OrderDTO>> byLink = list.stream().collect(Collectors.groupingBy(OrderDTO::getLink));
            List<String> resultOrdersString = byLink.entrySet().stream().map(entry -> {
                List<OrderDTO> orderList = entry.getValue();

                String modulesString = orderList.stream().map(order -> {
                    return order.getModuleName();
                }).distinct().collect(Collectors.joining(", "));
                SourceSiteDTO s = orderList.get(0).getSourceSite();
                return createOrdersMessage(modulesString, orderList.get(0).getTitle(), orderList.get(0).getLink(), s.getCategoryName(),
                        s.getSubCategoryName());
            }).toList();
            sendMessagesToUser(user, resultOrdersString);
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

    private void sendMessagesToUser(AppUser user, List<String> messages) {
        // Проверяем, нужно ли использовать email из-за ошибок Telegram
        boolean useEmail = false;

        if (!user.isDefaultSendType()) {
            Integer failCount = user.getTelegramFailCount();
            if (failCount != null && failCount >= maxTelegramFailures) {
                log.info("User {} has {} Telegram failures, using email",
                        user.getUuid(), failCount);
                useEmail = true;

                // Отправляем уведомление о проблеме (только если еще не отправляли в этой сессии)
                sendTelegramIssueNotification(user);
            }
        }

        // Выбираем способ отправки
        String uri = user.isDefaultSendType() || useEmail ? SEND_MESSAGE_URL_MAIL : SEND_MESSAGE_URL_TELEGRAM;

        // Отправляем сообщения
        boolean success = sendMessageBatch(user, uri, messages);

        // Обрабатываем результат
        if (!success && uri.equals(SEND_MESSAGE_URL_TELEGRAM)) {
            // Ошибка Telegram
            int newCount = user.getTelegramFailCount() == null ? 1 : user.getTelegramFailCount() + 1;
            user.setTelegramFailCount(newCount);
            userRepository.save(user);

            log.info("Telegram send failed for user {}, fail count: {}",
                    user.getUuid(), newCount);

            if (newCount >= maxTelegramFailures) {
                log.warn("User {} reached {} Telegram failures",
                        user.getUuid(), maxTelegramFailures);
            }
        } else if (success && uri.equals(SEND_MESSAGE_URL_TELEGRAM)) {
            // Успех Telegram - сбрасываем счетчик
            if (user.getTelegramFailCount() != null && user.getTelegramFailCount() > 0) {
                user.setTelegramFailCount(0);
                userRepository.save(user);
                log.info("Telegram success for user {}, reset fail count", user.getUuid());
            }
        }
    }

    private boolean sendMessageBatch(AppUser user, String uri, List<String> messages) {
        try {
            StringBuilder sb = new StringBuilder();
            for (String msg : messages) {
                sb.append(msg).append(NEW_LINE);
                if (sb.length() > 3000) {
                    if (!sendSingleMessage(user, uri, sb.substring(0, sb.length() - 1))) {
                        return false;
                    }
                    sb.setLength(0);
                }
            }
            if (sb.length() > 0) {
                return sendSingleMessage(user, uri, sb.substring(0, sb.length() - 1));
            }
            return true;
        } catch (Exception e) {
            log.debug("Error sending message batch to user {}: {}", user.getUuid(), e.getMessage());
            return false;
        }
    }


    private boolean sendSingleMessage(AppUser user, String uri, String message) {
        UserNotification un;

        if (uri.equals(SEND_MESSAGE_URL_MAIL)) {
            un = new UserNotification(user.getEmail(), message);
        } else {
            un = new UserNotification(String.valueOf(user.getTelegram()), message);
        }

        try {
            // Убираем .block() и используем синхронный подход
            // В WebFlux нельзя использовать .block() в реактивных потоках

            // Вариант 1: Просто отправляем без ожидания результата
            webClient.post()
                    .uri(uri)
                    .bodyValue(un)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .subscribe(
                            success -> log.debug("Message sent successfully to user {} via {}",
                                    user.getUuid(), uri.contains("telegram") ? "Telegram" : "Email"),
                            error -> log.debug("Failed to send message to user {} via {}: {}",
                                    user.getUuid(), uri.contains("telegram") ? "Telegram" : "Email", error.getMessage())
                    );

            return true; // Предполагаем успех
        } catch (Exception ex) {
            log.debug("Failed to send message to user {} via {}: {}",
                    user.getUuid(), uri.contains("telegram") ? "Telegram" : "Email", ex.getMessage());
            return false;
        }
    }

    private void sendTelegramIssueNotification(AppUser user) {
        UserNotification notification = new UserNotification(user.getEmail(), TELEGRAM_WARNING_MESSAGE);

        // Убираем .block() - отправляем асинхронно
        webClient.post()
                .uri(SEND_MESSAGE_URL_MAIL)
                .bodyValue(notification)
                .retrieve()
                .bodyToMono(Void.class)
                .subscribe(
                        success -> log.info("Sent Telegram issue notification to user {}", user.getUuid()),
                        error -> log.error("Failed to send Telegram issue notification to user {}", user.getUuid(), error)
                );
    }


    /*private boolean sendSingleMessage(AppUser user, String uri, String message) {
        UserNotification un;

        if (uri.equals(SEND_MESSAGE_URL_MAIL)) {
            un = new UserNotification(user.getEmail(), message);
        } else {
            un = new UserNotification(String.valueOf(user.getTelegram()), message);
        }

        try {
            webClient.post()
                    .uri(uri)
                    .bodyValue(un)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();

            log.debug("Message sent successfully to user {} via {}",
                    user.getUuid(), uri.contains("telegram") ? "Telegram" : "Email");
            return true;
        } catch (Exception ex) {
            log.debug("Failed to send message to user {} via {}: {}",
                    user.getUuid(), uri.contains("telegram") ? "Telegram" : "Email", ex.getMessage());
            return false;
        }
    }

    private void sendTelegramIssueNotification(AppUser user) {
        UserNotification notification = new UserNotification(user.getEmail(), TELEGRAM_WARNING_MESSAGE);

        try {
            webClient.post()
                    .uri(SEND_MESSAGE_URL_MAIL)
                    .bodyValue(notification)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();

            log.info("Sent Telegram issue notification to user {}", user.getUuid());
        } catch (Exception e) {
            log.error("Failed to send Telegram issue notification to user {}", user.getUuid(), e);
        }
    }*/

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

                    String modulesString = orderList.stream().map(order -> {
                        return order.getOrderName();
                    }).distinct().collect(Collectors.joining(", "));
                    return createOrdersMessage(modulesString, orderList.get(0).getTitle(), orderList.get(0).getLink(), orderList.get(0).getCategoryName(),
                            orderList.get(0).getSubCategoryName());
                }).toList();
                if (!resultOrdersString.isEmpty()) {
                    sendMessagesToUser(user, resultOrdersString);
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

    private String createOrdersMessage(String name, String title, String link, String categoryName,
                                       String subCategoryName) {
        return StringUtils.isNotEmpty(subCategoryName)
                ? String.format("%s, %s, %s новый заказ - %s %s", name, categoryName, subCategoryName, title, link)
                : String.format("%s, %s, новый заказ - %s %s", name, categoryName, title, link);
    }
}