package by.gdev.alert.job.parser.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import by.gdev.alert.job.parser.domain.db.ParserSource;

public interface ParserSourceRepository extends CrudRepository<ParserSource, Long>{
	
	Optional<ParserSource> findBySourceAndCategoryAndSubCategory(Long source, Long category, Long subCategory);
	
	@Query("select p from parser_order_source p left join fetch p.orders where p.source = :source and p.category = :category and p.subCategory = :subcategory")	
	Optional<ParserSource> findOneEagerOrders(@Param("source") Long source, @Param("category") Long category, @Param("subcategory") Long subcategory);
}