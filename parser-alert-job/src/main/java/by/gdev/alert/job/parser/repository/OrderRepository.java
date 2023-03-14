package by.gdev.alert.job.parser.repository;

import java.util.Set;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import by.gdev.alert.job.parser.domain.db.Order;

public interface OrderRepository extends CrudRepository<Order, Long>{
	
	@Query("select o from parser_order o left join fetch o.technologies left join fetch o.sourceSite p "
			+ "where p.source = :source and p.category = :category and (:subcategory is null or p.subCategory = :subcategory)")	
	Set<Order> findAllBySourceOneEagerTechnologies(@Param("source") Long source, @Param("category") Long category, @Param("subcategory") Long subcategory);

}