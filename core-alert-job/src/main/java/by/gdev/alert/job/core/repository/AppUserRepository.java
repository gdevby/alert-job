package by.gdev.alert.job.core.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import by.gdev.alert.job.core.model.db.AppUser;

public interface AppUserRepository extends CrudRepository<AppUser, Long> {
	
	Optional<AppUser> findByUuid(String uuid);
	
	@Query("select u from AppUser u left join fetch u.filters where u.uuid = :uuid")
	Optional<AppUser> findOneEagerUserFilters(@Param("uuid") String uuid);
	
	@Query("select u from AppUser u left join fetch u.filters left join fetch u.currentFilter where u.uuid = :uuid")
	Optional<AppUser> findOneEagerUserFiltersAndCurrentFilter(@Param("uuid") String uuid);
	
	@Query("select u from AppUser u left join fetch u.sources where u.uuid = :uuid")
	Optional<AppUser> findOneEagerSourceSite(@Param("uuid") String uuid);
}
