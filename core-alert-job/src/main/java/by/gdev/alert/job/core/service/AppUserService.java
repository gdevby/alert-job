package by.gdev.alert.job.core.service;

import by.gdev.alert.job.core.model.AppUserDTO;
import by.gdev.alert.job.core.model.db.AppUser;
import by.gdev.alert.job.core.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
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

}

