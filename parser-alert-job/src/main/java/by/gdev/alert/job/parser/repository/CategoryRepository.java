package by.gdev.alert.job.parser.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import by.gdev.alert.job.parser.domain.db.Category;

public interface CategoryRepository extends CrudRepository<Category, Long> {

	@Query("select c from parser_category c left join fetch c.siteSourceJob s where s.id = :id")
	List<Category> findAllBySourceId(@Param("id") Long id);

	@Query("select c from parser_category c left join fetch c.siteSourceJob s where s.id = :id and s.active = true")
	List<Category> findAllBySourceIdAndSourceActive(@Param("id") Long id);

	@Query("select c from parser_category c left join fetch c.siteSourceJob s where c.id = :id and s.id = :sid")
	Optional<Category> findByIdAndSourceId(@Param("id") Long id, @Param("sid") Long sid);

    @Modifying
    @Query("delete from parser_category c where c.siteSourceJob.id = :siteId")
    void deleteAllBySourceId(@Param("siteId") Long siteId);

}
