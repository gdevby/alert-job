package by.gdev.alert.job.core.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import by.gdev.alert.job.core.model.db.UserFilter;

public interface UserFilterRepository extends CrudRepository<UserFilter, Long> {

	@Query("select f from UserFilter f left join fetch f.titles where f.id = :id")
	Optional<UserFilter> findOneEagerTitleWords(@Param("id") Long id);

	@Query("select f from UserFilter f left join fetch f.technologies where f.id = :id")
	Optional<UserFilter> findOneEagerTechnologyWords(@Param("id") Long id);
	
	@Query("select f from UserFilter f left join fetch f.descriptions where f.id = :id")
	Optional<UserFilter> findOneEagerDescriptionWords(@Param("id") Long id);
}
