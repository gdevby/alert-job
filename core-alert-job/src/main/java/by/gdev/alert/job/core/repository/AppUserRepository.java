package by.gdev.alert.job.core.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import by.gdev.alert.job.core.model.db.AppUser;

public interface AppUserRepository extends CrudRepository<AppUser, Long> {
	
	Optional<AppUser> findByUuid(String uuid);
	
	@Query("select u from AppUser u left join fetch u.orderModules")
	List<AppUser> findAllUsersEagerOrderModules();
}
