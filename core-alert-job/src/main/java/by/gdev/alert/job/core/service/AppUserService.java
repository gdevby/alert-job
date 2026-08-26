package by.gdev.alert.job.core.service;

import by.gdev.alert.job.core.model.AppUserDTO;
import by.gdev.alert.job.core.model.db.AppUser;
import by.gdev.alert.job.core.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppUserService {
    private final AppUserRepository appUserRepository;
    private final IpGeoService ipGeoService;

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

    @Transactional
    public void updateUserIpAndCountry(String uuid, String ip) {
        if (uuid == null || ip == null) return;
        appUserRepository.findByUuid(uuid).ifPresent(user -> {
            // Определяем страну только если IP изменился или страна null
            boolean needUpdate = false;
            if (!ip.equals(user.getIpAddress())) {
                user.setIpAddress(ip);
                needUpdate = true;
            }
            if (user.getCountry() == null) {
                // Если страна ещё не определена, получаем её
                String country = ipGeoService.getCountryByIp(ip);
                if (country != null) {
                    user.setCountry(country);
                    needUpdate = true;
                }
            }
            if (needUpdate) {
                appUserRepository.save(user);
                log.debug("Updated user {} IP={}, country={}", uuid, user.getIpAddress(), user.getCountry());
            }
        });
    }

}

