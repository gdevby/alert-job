package by.gdev.alert.job.core.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import by.gdev.alert.job.core.model.db.OrderModules;
import by.gdev.alert.job.core.model.db.SourceSite;

public interface OrderModulesRepository extends CrudRepository<OrderModules, Long> {

	boolean existsBySources(SourceSite sources);

	boolean existsBySourcesAndSourcesActive(SourceSite sources, boolean active);

	boolean existsByNameAndUserUuid(String name, String uuid);

	Optional<OrderModules> findByIdAndUserUuid(Long id, String uuid);

	List<OrderModules> findAllByUserUuid(String uuid);

	@Query("select o from OrderModules o left join fetch o.filters left join fetch o.user u where o.id = :id and u.uuid = :uuid")
	Optional<OrderModules> findByIdAndUserUuidOneEagerFilters(Long id, String uuid);

	@Query("select o from OrderModules o left join fetch o.sources left join fetch o.user u where o.id = :id and u.uuid = :uuid")
	Optional<OrderModules> findByIdAndUserUuidOneEagerSources(Long id, String uuid);

	@Query("select module from OrderModules module left join fetch module.user user"
			+ " left join fetch module.currentFilter filter"
			+ " left join fetch filter.descriptionWordPrice left join fetch filter.technologies"
			+ " left join fetch filter.titles left join fetch filter.descriptions"
			+ " left join fetch filter.negativeTechnologies left join fetch filter.negativeTitles"
			+ " left join fetch filter.negativeDescriptions where module.id = :id and user.uuid = :uuid")
	Optional<OrderModules> findByIdAndUserUuidOneEagerCurrentFilter(Long id, String uuid);
}