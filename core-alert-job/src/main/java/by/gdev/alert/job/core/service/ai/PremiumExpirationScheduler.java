package by.gdev.alert.job.core.service.ai;

import by.gdev.alert.job.core.model.db.AppUser;
import by.gdev.alert.job.core.repository.AppUserRepository;
import by.gdev.alert.job.core.repository.OrderModulesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PremiumExpirationScheduler {

    private final AppUserRepository appUserRepository;
    private final OrderModulesRepository orderModulesRepository;

    @Value("${premium.duration.days:30}")
    private int premiumDurationDays;


    //выключаем по факту при созаднии пользователем всем премиум
    @Scheduled(fixedDelay = 7200000) // 2 часа
    @Transactional
    public void checkExpiredPremium() {
        log.info("АВТООТВЕТ: SCHEDULER -> проверка истекших премиумов");

        List<AppUser> users = appUserRepository.findAllByPremiumTrue();

        for (AppUser user : users) {
            if (user.getPremiumStartedAt() == null) continue;

            LocalDateTime expiresAt = user.getPremiumStartedAt().plusDays(premiumDurationDays);
            if (expiresAt.isBefore(LocalDateTime.now())) {
                log.warn("АВТООТВЕТ: SCHEDULER -> ПРЕМИУМ ИСТЕК у пользователя: {}", user.getEmail());

                // Отключаем премиум
                user.setPremium(false);
                user.setPremiumStartedAt(null);
                appUserRepository.save(user);

                // Отключаем все автоответы пользователя
                orderModulesRepository.updateAutoReplyEnabledByUserUuid(user.getUuid(), false);

                // TODO: отправить уведомление пользователю
            }
        }
    }
}