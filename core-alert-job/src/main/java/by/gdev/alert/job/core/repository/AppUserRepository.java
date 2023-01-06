package by.gdev.alert.job.core.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import by.gdev.alert.job.core.model.db.AppUser;

public interface AppUserRepository extends CrudRepository<AppUser, Long> {
	
	Optional<AppUser> findByUuid(String uuid);

}
