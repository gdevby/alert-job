package by.gdev.alert.job.core.service.ai;

import by.gdev.alert.job.core.model.db.AppUser;
import by.gdev.alert.job.core.model.db.OrderModules;
import by.gdev.alert.job.core.repository.AppUserRepository;
import by.gdev.alert.job.core.repository.OrderModulesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiFilterService {

    private final OrderModulesRepository orderModulesRepository;
    private final AppUserRepository appUserRepository;

    @Value("${premium.duration.days:30}")
    private int premiumDurationDays;

    public boolean getAutoReplyStatus(String uuid, Long moduleId) {
        Optional<OrderModules> orderModule = orderModulesRepository.findById(moduleId);
        return orderModule.isPresent() && Boolean.TRUE.equals(orderModule.get().getAutoReplyEnabled());
    }

    @Transactional
    public void setAutoReplyStatus(String uuid, Long moduleId, boolean status) {
        Optional<OrderModules> orderModuleOptional = orderModulesRepository.findById(moduleId);
        if (orderModuleOptional.isPresent()) {
            OrderModules orderModule = orderModuleOptional.get();
            if (orderModule.isAvailable()) {
                orderModule.setAutoReplyEnabled(status);
                orderModulesRepository.save(orderModule);
                // Обновляем премиум статус
                updatePremiumStatus(uuid);
                log.info("АВТООТВЕТ: пользователь {} {} автоответ на модуле {}",
                        uuid, status ? "включил" : "отключил", moduleId);
            }
        }
    }

    private void updatePremiumStatus(String uuid) {
        AppUser user = appUserRepository.findByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        boolean anyAutoReplyEnabled = orderModulesRepository.existsByUserUuidAndAutoReplyEnabledTrue(uuid);

        if (anyAutoReplyEnabled && !user.isPremium()) {
            user.setPremium(true);
            user.setPremiumStartedAt(LocalDateTime.now());
            log.info("АВТООТВЕТ: пользователь {} стал премиумом", uuid);
        } else if (!anyAutoReplyEnabled && user.isPremium()) {
            user.setPremium(false);
            user.setPremiumStartedAt(null);
            log.info("АВТООТВЕТ: пользователь {} потерял премиум (все автоответы выключены)", uuid);
        }
        appUserRepository.save(user);
    }

    public boolean isPremium(String uuid) {
        AppUser user = appUserRepository.findByUuid(uuid).orElse(null);
        if (user == null) return false;
        if (!user.isPremium()) return false;
        if (user.getPremiumStartedAt() == null) return false;
        // Проверяем, не истек ли премиум
        LocalDateTime expiresAt = user.getPremiumStartedAt().plusDays(premiumDurationDays);
        return expiresAt.isAfter(LocalDateTime.now());
    }
}