package by.gdev.alert.job.core.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import by.gdev.alert.job.core.model.db.AppUser;

public interface AppUserRepository extends CrudRepository<AppUser, Long> {
	
	Optional<AppUser> findByUuid(String uuid);
	
	@Query("select u from AppUser u left join fetch u.orderModules where u.uuid = :uuid")
	Optional<AppUser> findOneEagerOrderModules(String uuid);
	
	@Query("select u from AppUser u left join fetch u.orderModules o left join fetch o.filters where u.uuid = :uuid and o.id = :oid")
	Optional<AppUser> findByUuidAndOrderModulesIdOneEagerFilters(@Param("uuid") String uuid, @Param("oid") Long oid);
	
	@Query("select u from AppUser u left join fetch u.orderModules o left join fetch o.sources where u.uuid = :uuid and o.id = :oid")
	Optional<AppUser> findByUuidAndOrderModulesIdOneEagerSources(@Param("uuid") String uuid, @Param("oid") Long oid);
	
	@Query("select u from AppUser u left join fetch u.orderModules o left join fetch o.currentFilter where u.uuid = :uuid and o.id = :oid")
	Optional<AppUser> findByUuidAndOrderModulesIdOneEagerCurrentFilter(@Param("uuid") String uuid, @Param("oid") Long oid);
	
	@Query("select u from AppUser u left join fetch u.orderModules o where u.uuid = :uuid and o.id = :oid")
	Optional<AppUser> findByUuidAndOrderModulesId(@Param("uuid") String uuid, @Param("oid") Long oid);
	
	@Query("select u from AppUser u left join fetch u.orderModules")
	List<AppUser> findAllUsersEagerOrderModules();
}
