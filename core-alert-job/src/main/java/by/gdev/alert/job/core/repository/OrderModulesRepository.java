package by.gdev.alert.job.core.repository;

import by.gdev.alert.job.core.model.db.OrderModules;
import by.gdev.alert.job.core.model.db.SourceSite;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderModulesRepository extends CrudRepository<OrderModules, Long> {

	boolean existsBySources(SourceSite sources);

	boolean existsBySourcesAndSourcesActive(SourceSite sources, boolean active);

	boolean existsByNameAndUserUuid(String name, String uuid);

	Optional<OrderModules> findByIdAndUserUuid(Long id, String uuid);

	List<OrderModules> findAllByUserUuid(String uuid);

	@Query("select o from OrderModules o left join fetch o.filters left join fetch o.user u where o.id = :id and u.uuid = :uuid")
	Optional<OrderModules> findByIdAndUserUuidOneEagerFilters(@Param("id")Long id, @Param("uuid")String uuid);

	@Query("select o from OrderModules o left join fetch o.sources left join fetch o.user u where o.id = :id and u.uuid = :uuid")
	Optional<OrderModules> findByIdAndUserUuidOneEagerSources(@Param("id")Long id, @Param("uuid")String uuid);

	@Query("select module from OrderModules module where module.id = :id and user.uuid = :uuid")
	Optional<OrderModules> findByIdAndUserUuidOneEagerCurrentFilter(@Param("id")Long id, @Param("uuid")String uuid);

    @Query("""
    select m
    from OrderModules m
    join m.sources s
    where s.id = :sourceId
""")
    List<OrderModules> findAllBySourceId(@Param("sourceId") Long sourceId);

}