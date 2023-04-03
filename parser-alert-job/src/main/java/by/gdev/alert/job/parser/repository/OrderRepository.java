package by.gdev.alert.job.parser.repository;

import java.util.Set;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import by.gdev.alert.job.parser.domain.db.Order;

public interface OrderRepository extends CrudRepository<Order, Long> {

    @Query("select o from parser_order o left join fetch o.technologies left join fetch o.sourceSite s "
	    + "where s.source = :source and s.category = :category and subCategory = :subCategory")
    Set<Order> findAllBySourceOneEagerTechnologies(Long source, Long category, Long subCategory);

    @Query("select o from parser_order o left join fetch o.technologies left join fetch o.sourceSite s "
	    + "where s.source = :source and s.category = :category and s.subCategory is null")
    Set<Order> findAllBySourceSubCategoryIsNullOneEagerTechnologies(Long source, Long category);
}