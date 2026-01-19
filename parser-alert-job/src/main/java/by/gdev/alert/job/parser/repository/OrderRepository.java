package by.gdev.alert.job.parser.repository;

import java.util.Date;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import by.gdev.alert.job.parser.domain.db.Order;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

public interface OrderRepository extends CrudRepository<Order, Long> {

    @Query("select o from parser_order o left join fetch o.sourceSite s "
            + "where s.source = :source and s.category = :category and s.subCategory = :subCategory and o.dateTime >= :date")
    Set<Order> findAllBySourceOneEager(Long source, Long category, Long subCategory, Date date);

    @Query("select o from parser_order o left join fetch o.sourceSite s "
            + "where s.source = :source and s.category = :category and s.subCategory is null and o.dateTime >= :date")
    Set<Order> findAllBySourceSubCategoryIsNullOneEager(Long source, Long category, Date date);

    Optional<Order> findByLink(String link);

    /**
     * Проверяет существование заказа по link, category и subCategory
     */
    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN TRUE ELSE FALSE END " +
            "FROM parser_order o " +
            "JOIN o.sourceSite s " +
            "WHERE o.link = :link " +
            "AND s.category = :category " +
            "AND (:subCategory IS NULL AND s.subCategory IS NULL " +
            "     OR s.subCategory = :subCategory)")
    boolean existsByLinkCategoryAndSubCategory(
            @Param("link") String link,
            @Param("category") Long category,
            @Param("subCategory") Long subCategory);

    @Modifying
    @Transactional(timeout = 500)
    long deleteByDateTimeBefore(Date cutoff);

}
