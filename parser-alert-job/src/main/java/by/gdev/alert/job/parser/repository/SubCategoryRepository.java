package by.gdev.alert.job.parser.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import by.gdev.alert.job.parser.domain.db.Subcategory;

public interface SubCategoryRepository extends CrudRepository<Subcategory, Long> {
	
	@Query("select sc from parser_sub_category sc left join fetch sc.category c where c.id = :id")
	List<Subcategory> findAllByCategoryId(@Param("id") Long id);
	
	@Query("select sc from parser_sub_category sc left join fetch sc.category c where sc.id = :id and c.id = :cid")
	Optional<Subcategory> findByIdAndCategoryId(@Param("id") Long id, @Param("cid") Long cid);
}