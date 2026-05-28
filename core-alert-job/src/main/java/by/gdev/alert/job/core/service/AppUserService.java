package by.gdev.alert.job.core.service;

import by.gdev.alert.job.core.model.db.AppUser;
import by.gdev.alert.job.core.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AppUserService {
    private final AppUserRepository repository;

    public Optional<AppUser> findByUuid(String uuid) {
        return repository.findByUuid(uuid);
    }
}

