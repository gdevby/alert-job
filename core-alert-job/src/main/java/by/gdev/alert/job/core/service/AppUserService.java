package by.gdev.alert.job.core.service;

import by.gdev.alert.job.core.configuration.category.AdminProperties;
import by.gdev.alert.job.core.model.AppUserDTO;
import by.gdev.alert.job.core.model.ModuleSiteDto;
import by.gdev.alert.job.core.model.db.AppUser;
import by.gdev.alert.job.core.model.db.OrderModules;
import by.gdev.alert.job.core.model.db.SourceSite;
import by.gdev.alert.job.core.repository.AppUserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppUserService {
    private final AppUserRepository appUserRepository;
    private final AdminProperties adminProperties;

    @Value("${alert.admin.uuids:}")
    private String adminUuidsRaw;

    private Set<String> adminUuids;

    @Value("${premium.duration.days:30}")
    private int premiumDurationDays;

    @PostConstruct
    public void init() {
        if (adminUuidsRaw != null && !adminUuidsRaw.isBlank()) {
            adminUuids = Arrays.stream(adminUuidsRaw.split(","))
                    .map(String::trim)
                    .collect(Collectors.toSet());
            log.info("Загружены администраторы: {}", adminUuids);
        } else {
            adminUuids = Set.of();
        }
    }

    public Optional<AppUser> findByUuid(String uuid) {
        return appUserRepository.findByUuid(uuid);
    }

    public List<AppUserDTO> findAllUsers() {
        return StreamSupport.stream(appUserRepository.findAll().spliterator(), false)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private AppUserDTO convertToDTO(AppUser user) {
        AppUserDTO dto = new AppUserDTO();
        dto.setUuid(user.getUuid());
        dto.setEmail(user.getEmail());
        dto.setTelegram(user.getTelegram());
        dto.setSwitchOffAlerts(user.isSwitchOffAlerts());
        dto.setDefaultSendType(user.isDefaultSendType());
        return dto;
    }

    /**
     * Проверяет, является ли пользователь администратором
     */
    public boolean isAdmin(AppUser user) {
        return adminUuids.contains(user.getUuid());
    }

    /**
     * Проверяет, имеет ли пользователь премиум-доступ по UUID
     */
    public boolean isPremiumUser(String uuid) {
        if (uuid == null || uuid.isBlank()) {
            return false;
        }
        AppUser user = appUserRepository.findByUuid(uuid).orElse(null);
        return isPremiumUser(user);
    }

    /**
     * Проверяет, имеет ли пользователь премиум-доступ
     * (с учётом администраторов, у которых вечный премиум)
     */
    public boolean isPremiumUser(AppUser user) {
        if (user == null) {
            return false;
        }

        // Администраторы имеют вечный премиум
        if (isAdmin(user)) {
            return true;
        }

        // Если premium == null — считаем, что пользователь премиум (для старых пользователей)
        if (user.getPremium() == null) {
            return true;
        }

        // Если premium == false — не премиум
        if (!Boolean.TRUE.equals(user.getPremium())) {
            return false;
        }

        // Проверяем дату начала
        if (user.getPremiumStartedAt() == null) {
            // Если premium = true, но даты нет — считаем премиумом (запасной вариант)
            return true;
        }

        // Проверяем, не истек ли срок премиума
        LocalDateTime expiresAt = user.getPremiumStartedAt().plusDays(premiumDurationDays);
        return expiresAt.isAfter(LocalDateTime.now());
    }

    /**
     * Получает список пользователей у которых включен премиум
     */
    public List<AppUser> getPremiumUsers() {
        return appUserRepository.findAllByPremiumTrue();
    }

    /**
     * Отключает премиум у пользователя
     */
    public void disablePremium(AppUser user) {
        // Отключаем премиум у пользователя
        user.setPremium(false);
        user.setPremiumStartedAt(null);
        appUserRepository.save(user);
    }

    public AppUser save(AppUser user) {
        return appUserRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<ModuleSiteDto> getAutoReplyEnabledModules(String uuid) {
        AppUser user = appUserRepository.findByUuidWithModulesAndSources(uuid)
                .orElseThrow(() -> new RuntimeException("User not found: " + uuid));
        if (user.getOrderModules() == null) return List.of();

        Set<ModuleSiteDto> result = new HashSet<>();
        for (OrderModules om : user.getOrderModules()) {
            if (om.getAutoReplyEnabled() != null && om.getAutoReplyEnabled()) {
                for (SourceSite source : om.getSources()) {
                    result.add(new ModuleSiteDto(om.getId(), source.getSiteSource()));
                }
            }
        }
        return new ArrayList<>(result);
    }

    public List<AppUser> findUsersBySourceSiteId(Long sourceSiteId){
        return appUserRepository.findUsersBySourceSiteId(sourceSiteId);
    }

    public List<AppUser> getAdminUsers() {
        List<AppUser> admins = new ArrayList<>();
        if (adminProperties.getUuids() == null) {
            return admins;
        }
        for (String uuid : adminProperties.getUuids()) {
            appUserRepository.findByUuid(uuid).ifPresent(admins::add);
        }
        return admins;
    }
}

