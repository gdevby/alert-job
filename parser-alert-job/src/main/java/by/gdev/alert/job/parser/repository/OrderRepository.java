package by.gdev.alert.job.parser.repository;

import java.util.Date;
import java.util.Set;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import by.gdev.alert.job.parser.domain.db.Order;

public interface OrderRepository extends CrudRepository<Order, Long> {

    @Query("select o from parser_order o left join fetch o.sourceSite s "
	    + "where s.source = :source and s.category = :category and s.subCategory = :subCategory and o.dateTime >= :date")
    Set<Order> findAllBySourceOneEager(Long source, Long category, Long subCategory, Date date);

    @Query("select o from parser_order o left join fetch o.sourceSite s "
	    + "where s.source = :source and s.category = :category and s.subCategory is null and o.dateTime >= :date")
    Set<Order> findAllBySourceSubCategoryIsNullOneEager(Long source, Long category, Date date);

    boolean existsByLink(String link);
}