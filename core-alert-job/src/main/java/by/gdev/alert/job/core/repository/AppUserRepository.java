package by.gdev.alert.job.core.repository;

import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import by.gdev.alert.job.core.model.db.AppUser;

public interface AppUserRepository extends CrudRepository<AppUser, Long> {

    Optional<AppUser> findByUuid(String uuid);

    @Query("select u from AppUser u left join fetch u.orderModules where u.uuid = :uuid")
    Optional<AppUser> findByUuidOneEagerModules(String uuid);

    @Query("select u from AppUser u left join fetch u.orderModules o left join fetch o.sources "
	    + "where u.switchOffAlerts = true and o.available = true")
    Set<AppUser> findAllUsersEagerOrderModules();
}