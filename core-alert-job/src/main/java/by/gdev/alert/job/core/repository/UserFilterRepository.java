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
	Optional<UserFilter> findByIdAndModuleIdAndUserUuid(@Param("id") Long id, @Param("mid") Long mid,
			@Param("uuid") String uuid);

	boolean existsByNameAndModule(String name, OrderModules module);

	@Query("select filter from UserFilter filter left join fetch filter.descriptionWordPrice where filter.id = :id")
	Optional<UserFilter> findByIdOneEagerDescriptionWordPrice(Long id);

	@Query("select filter from UserFilter filter left join fetch filter.titles where filter.id = :id")
	Optional<UserFilter> findByIdOneEagerTitles(Long id);

	@Query("select filter from UserFilter filter left join fetch filter.descriptions where filter.id = :id")
	Optional<UserFilter> findByIdOneEagerDescriptions(Long id);

	@Query("select filter from UserFilter filter left join fetch filter.negativeTitles where filter.id = :id")
	Optional<UserFilter> findByIdOneEagerNegativeTitles(Long id);

	@Query("select filter from UserFilter filter left join fetch filter.negativeDescriptions where filter.id = :id")
	Optional<UserFilter> findByIdOneEagerNegativeDescriptions(Long id);

	@Query("select filter from UserFilter filter left join fetch filter.descriptionWordPrice"
			+ " left join fetch filter.titles" + " left join fetch filter.descriptions"
			+ " left join fetch filter.negativeTitles left join fetch filter.negativeDescriptions"
			+ " where filter.id = :id")
	UserFilter findByIdEagerAllWords(Long id);

	@Query("select filter from UserFilter filter left join fetch filter.descriptionWordPrice"
			+ " left join fetch filter.titles" + " left join fetch filter.descriptions"
			+ " left join fetch filter.negativeTitles left join fetch filter.negativeDescriptions")
	List<UserFilter> findByIdEagerAllWordsAll();

}
