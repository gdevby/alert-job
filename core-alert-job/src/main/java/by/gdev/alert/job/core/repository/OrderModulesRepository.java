package by.gdev.alert.job.core.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import by.gdev.alert.job.core.model.db.OrderModules;
import by.gdev.alert.job.core.model.db.SourceSite;

public interface OrderModulesRepository extends CrudRepository<OrderModules, Long>{
	
	@Query("select o from OrderModules o left join fetch o.filters where o.id = :id")
	Optional<OrderModules> findOneEagerUserFilters(@Param("id") Long id);
	
	boolean existsBySources(SourceSite sources);
}