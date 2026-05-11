package by.gdev.alert.job.parser.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import by.gdev.alert.job.parser.domain.db.Category;
import org.springframework.transaction.annotation.Transactional;

public interface CategoryRepository extends CrudRepository<Category, Long> {

	@Query("select c from parser_category c left join fetch c.siteSourceJob s where s.id = :id")
	List<Category> findAllBySourceId(@Param("id") Long id);

	@Query("select c from parser_category c left join fetch c.siteSourceJob s where s.id = :id and s.active = true")
	List<Category> findAllBySourceIdAndSourceActive(@Param("id") Long id);

	@Query("select c from parser_category c left join fetch c.siteSourceJob s where c.id = :id and s.id = :sid")
	Optional<Category> findByIdAndSourceId(@Param("id") Long id, @Param("sid") Long sid);


    @Query("""
    SELECT c FROM parser_category c
    LEFT JOIN FETCH c.subCategories
    WHERE c.siteSourceJob.id = :siteId
""")
    List<Category> findAllWithSubcategoriesBySourceId(Long siteId);

    @Modifying
    @Transactional
    @Query(
            value = """
        UPDATE parser_sub_category 
        SET POSITION = 0 
        WHERE POSITION IS NULL 
          AND category_id IN (
              SELECT id FROM parser_category WHERE site_source_job_id = :siteId
          )
        """,
            nativeQuery = true
    )
    void fixBrokenSubcategoryPositions(Long siteId);



}
