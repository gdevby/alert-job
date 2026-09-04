package by.gdev.alert.job.core.service.ai;

import by.gdev.alert.job.core.model.db.AppUser;
import by.gdev.alert.job.core.client.NotificationClient;
import by.gdev.alert.job.core.model.db.OrderModules;
import by.gdev.alert.job.core.repository.OrderModulesRepository;
import by.gdev.alert.job.core.service.AppUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiFilterService {

    private final OrderModulesRepository orderModulesRepository;
    private final AppUserService appUserService;
    private final NotificationClient notificationClient;

    public boolean getAutoReplyStatus(String uuid, Long moduleId) {
        Optional<OrderModules> orderModule = orderModulesRepository.findByIdAndUserUuid(moduleId, uuid);
        return orderModule.isPresent() && Boolean.TRUE.equals(orderModule.get().getAutoReplyEnabled());
    }

    public void setAutoReplyStatus(String uuid, Long moduleId, boolean status) {
        Optional<OrderModules> orderModuleOptional = orderModulesRepository.findById(moduleId);
        if (orderModuleOptional.isPresent()) {
            OrderModules orderModule = orderModuleOptional.get();
            if (orderModule.isAvailable()) {
                // Если пытается включить автоответ, проверяем премиум
                if (status) {
                    // Проверяем, активен ли премиум (не истек)
                    if (!appUserService.isPremiumUser(uuid)) {
                        log.warn("АВТООТВЕТ: пользователь {} пытается включить автоответ, но премиум истек", uuid);
                        return;
                    }

                    // Проверяем, не включен ли уже автоответ
                    if (Boolean.TRUE.equals(orderModule.getAutoReplyEnabled())) {
                        log.warn("АВТООТВЕТ: пользователь {} пытается повторно включить автоответ на модуле {}, уже включен",
                                uuid, moduleId);
                        return;
                    }
                }

                orderModule.setAutoReplyEnabled(status);
                orderModulesRepository.save(orderModule);
                updatePremiumStatus(uuid);
                // Если автоответ включён – перераспределяем прокси
                if (status) {
                    notificationClient.reassignProxies();
                }
                log.info("АВТООТВЕТ: пользователь {} {} автоответ на модуле {}",
                        uuid, status ? "включил" : "отключил", moduleId);
            }
        }
    }

    private void updatePremiumStatus(String uuid) {
        AppUser user = appUserService.findByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        boolean anyAutoReplyEnabled = orderModulesRepository.existsByUserUuidAndAutoReplyEnabledTrue(uuid);

        if (anyAutoReplyEnabled && !Boolean.TRUE.equals(user.getPremium())) {
            user.setPremium(true);
            user.setPremiumStartedAt(LocalDateTime.now());
            log.info("АВТООТВЕТ: пользователь {} стал премиумом", uuid);
        } else if (!anyAutoReplyEnabled && Boolean.TRUE.equals(user.getPremium())) {
            user.setPremium(false);
            user.setPremiumStartedAt(null);
            log.info("АВТООТВЕТ: пользователь {} потерял премиум (все автоответы выключены)", uuid);
        }
        appUserService.save(user);
    }

    public boolean isPremium(String uuid) {
        return appUserService.isPremiumUser(uuid);
    }
}