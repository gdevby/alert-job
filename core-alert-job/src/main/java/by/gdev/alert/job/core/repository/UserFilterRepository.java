package by.gdev.alert.job.core.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import by.gdev.alert.job.core.model.db.OrderModules;
import by.gdev.alert.job.core.model.db.UserFilter;

public interface UserFilterRepository extends CrudRepository<UserFilter, Long> {

	Optional<UserFilter> findByIdAndModuleId(@Param("id") Long id, @Param("mid") Long mid);
	
	@Query("select f from UserFilter f left join fetch f.module m left join fetch m.user u where m.id = :mid and u.uuid = :uuid")
	List<UserFilter> findAllByModuleIdAndUserUuid(@Param("mid") Long mid, @Param("uuid") String uuid);
	
	@Query("select f from UserFilter f left join fetch f.module m left join fetch m.user u where f.id = :id and m.id = :mid and u.uuid = :uuid")
	Optional<UserFilter> findByIdAndModuleIdAndUserUuid(@Param("id") Long id, @Param("mid") Long mid, @Param("uuid") String uuid);
	
	boolean existsByNameAndModule(String name, OrderModules module);
	
}
