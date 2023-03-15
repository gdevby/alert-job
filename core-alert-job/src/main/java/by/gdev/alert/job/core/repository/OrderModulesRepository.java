package by.gdev.alert.job.core.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import by.gdev.alert.job.core.model.db.OrderModules;
import by.gdev.alert.job.core.model.db.SourceSite;

public interface OrderModulesRepository extends CrudRepository<OrderModules, Long>{
	
	boolean existsBySources(SourceSite sources);
	
	boolean existsByNameAndUserUuid(String name, String uuid);
	
	Optional<OrderModules> findByIdAndUserUuid(Long id, String uuid);
	
	List<OrderModules> findAllByUserUuid(String uuid);
	
	@Query("select o from OrderModules o left join fetch o.sources left join fetch o.user u where o.id = :id and u.uuid = :uuid")
	Optional<OrderModules> findByIdAndUserUuidOneEagerSources(Long id, String uuid);
	
	@Query("select o from OrderModules o left join fetch o.currentFilter left join fetch o.user u where o.id = :id and u.uuid = :uuid")
	Optional<OrderModules> findByIdAndUserUuidOneEagerCurrentFilter(Long id, String uuid);
	
	
	@Query("select o from OrderModules o left join fetch o.filters f left join fetch o.user u where o.id = :id and u.uuid = :uuid and f.id = :fid")
	Optional<OrderModules> findByIdAndUserUuidOneEagerFilters(Long id, String uuid, Long fid);
	
	
	
}