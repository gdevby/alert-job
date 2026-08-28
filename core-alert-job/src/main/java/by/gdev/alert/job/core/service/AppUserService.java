package by.gdev.alert.job.core.service;

import by.gdev.alert.job.core.model.AppUserDTO;
import by.gdev.alert.job.core.model.ModuleSiteDto;
import by.gdev.alert.job.core.model.db.AppUser;
import by.gdev.alert.job.core.model.db.OrderModules;
import by.gdev.alert.job.core.model.db.SourceSite;
import by.gdev.alert.job.core.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppUserService {
    private final AppUserRepository appUserRepository;

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
}

