package by.gdev.alert.job.core.service.ai;

import by.gdev.alert.job.core.model.db.AppUser;
import by.gdev.alert.job.core.model.db.OrderModules;
import by.gdev.alert.job.core.repository.OrderModulesRepository;
import by.gdev.alert.job.core.service.AppUserService;
import by.gdev.alert.job.core.service.MailSenderService;
import by.gdev.alert.job.core.templates.MessageTemplates;
import by.gdev.common.model.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PremiumExpirationScheduler {

    private final AppUserService appUserService;
    private final OrderModulesRepository orderModulesRepository;
    private final MailSenderService mailSenderService;

    @Value("${premium.duration.days:0}")
    private int premiumDurationDays;

    @Scheduled(fixedDelay = 7200000) // 2 часа
    @Transactional
    public void checkExpiredPremium() {
        log.info("АВТООТВЕТ: SCHEDULER -> проверка истекших премиумов");
        List<AppUser> premiumUsers = getPremiumUsers();
        if (premiumUsers.isEmpty()) {
            log.debug("АВТООТВЕТ: SCHEDULER -> пользователей с премиумом нет");
            return;
        }

        for (AppUser user : premiumUsers) {
            if (appUserService.isAdmin(user)) {
                continue;
            }
            processUserPremium(user);
        }
    }

    /**
     * Получает список пользователей у которых включен премиум
     */
    protected List<AppUser> getPremiumUsers() {
        return appUserService.getPremiumUsers();
    }

    /**
     * Проверяет активность премиума у пользователя
     */
    protected boolean isPremiumActive(AppUser user) {
        if (user.getPremiumStartedAt() == null) {
            return false;
        }
        LocalDateTime expiresAt = user.getPremiumStartedAt().plusDays(premiumDurationDays);
        return expiresAt.isAfter(LocalDateTime.now());
    }

    /**
     * Проверяет, истек ли премиум у пользователя
     */
    protected boolean isPremiumExpired(AppUser user) {
        return !isPremiumActive(user);
    }

    /**
     * Обрабатывает одного пользователя: проверяет, отключает и отправляет уведомление
     */
    protected void processUserPremium(AppUser user) {
        if (!isPremiumExpired(user)) {
            return;
        }

        log.warn("АВТООТВЕТ: SCHEDULER -> ПРЕМИУМ ИСТЕК у пользователя: {}", user.getEmail());

        // Сохраняем данные для уведомления ДО отключения
        LocalDateTime startedAt = user.getPremiumStartedAt();
        String modulesList = collectModulesInfo(user);

        // Отключаем премиум
        disablePremium(user);

        // Отключаем все автоответы
        orderModulesRepository.updateAutoReplyEnabledByUserUuid(user.getUuid(), false);

        // Отправляем уведомление
        sendExpiredNotification(user, startedAt, modulesList);
    }

    /**
     * Собирает информацию о модулях с включенным автоответом
     */
    protected String collectModulesInfo(AppUser user) {
        List<OrderModules> modules = orderModulesRepository.findByUserUuidAndAutoReplyEnabledTrue(user.getUuid());
        return modules.stream()
                .map(OrderModules::getName)
                .collect(Collectors.joining(", "));
    }

    /**
     * Отключает премиум у пользователя
     */
    protected void disablePremium(AppUser user) {
        appUserService.disablePremium(user);
        // Отключаем все автоответы у модулей пользователя
        orderModulesRepository.updateAutoReplyEnabledByUserUuid(user.getUuid(), false);
        log.info("АВТООТВЕТ: SCHEDULER -> премиум и автоответы отключены у пользователя: {}", user.getEmail());
    }

    /**
     * Отправляет уведомление об истечении премиума
     */
    protected void sendExpiredNotification(AppUser user, LocalDateTime startedAt, String modulesList) {
        if (!user.isSwitchOffAlerts()) {
            log.debug("АВТООТВЕТ: SCHEDULER -> уведомления отключены у пользователя: {}", user.getEmail());
            return;
        }

        String startedAtStr = startedAt != null
                ? startedAt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
                : "неизвестно";
        String expiredAtStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));

        String htmlMessage = MessageTemplates.Premium.buildExpiredHtml(
                user.getEmail(),
                startedAtStr,
                modulesList,
                expiredAtStr
        );

        String telegramMessage = MessageTemplates.Premium.buildExpiredTelegram(
                user.getEmail(),
                startedAtStr,
                modulesList,
                expiredAtStr
        );

        String message = user.isDefaultSendType() ? htmlMessage : telegramMessage;
        mailSenderService.sendMessagesToUser(user, List.of(message), NotificationType.PREMIUM_EXPIRED);

        log.info("АВТООТВЕТ: SCHEDULER -> уведомление отправлено пользователю: {}", user.getEmail());
    }
}