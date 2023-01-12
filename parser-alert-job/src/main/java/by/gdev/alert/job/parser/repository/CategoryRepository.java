package by.gdev.alert.job.parser.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import by.gdev.alert.job.parser.domain.db.Category;

public interface CategoryRepository extends CrudRepository<Category, Long>{
	
	@Query("select c from parser_category c left join fetch c.subCategories where c.id = :id")
	Optional<Category> findOneEager(@Param("id") Long id);
	
	@Query("select c from parser_category c left join fetch c.subCategories s where c.id= :id and s.id = :sId")
	Category findByIdAndSubCategory(@Param("id") Long id, @Param("sId") Long sId);
	

}
