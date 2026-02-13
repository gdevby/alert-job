package by.gdev.alert.job.parser.repository;

import by.gdev.alert.job.parser.domain.db.Order;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderSearchRepository extends CrudRepository<Order, Long> {

    @Query(value = """
SELECT o.*
FROM parser_order o
JOIN parser_order_source ps ON ps.id = o.source_site_id
WHERE ps.source IN (:sites)
  AND (
        (:booleanQuery IS NULL AND :likeQuery IS NULL)
        OR (
            :booleanQuery IS NOT NULL AND (
                CASE
                    WHEN :mode = 'TITLE' THEN MATCH(o.title) AGAINST (:booleanQuery IN BOOLEAN MODE)
                    WHEN :mode = 'DESCRIPTION' THEN MATCH(o.message) AGAINST (:booleanQuery IN BOOLEAN MODE)
                    ELSE MATCH(o.title, o.message) AGAINST (:booleanQuery IN BOOLEAN MODE)
                END
            )
        )
        OR (
            :likeQuery IS NOT NULL AND (
                o.title LIKE CONCAT('%', :likeQuery, '%')
                OR o.message LIKE CONCAT('%', :likeQuery, '%')
            )
        )
      )
ORDER BY o.date_time DESC
LIMIT :offset, :size
""", nativeQuery = true)
    List<Order> searchOrders(
            @Param("sites") List<Long> siteIds,
            @Param("mode") String mode,
            @Param("booleanQuery") String booleanQuery,
            @Param("likeQuery") String likeQuery,
            @Param("offset") int offset,
            @Param("size") int size
    );

    @Query(value = """
SELECT COUNT(*)
FROM parser_order o
JOIN parser_order_source ps ON ps.id = o.source_site_id
WHERE ps.source IN (:sites)
  AND (
        -- 1) Если нет ни booleanQuery, ни likeQuery → вернуть ВСЕ заказы
        (:booleanQuery IS NULL AND :likeQuery IS NULL)

        -- 2) Если есть booleanQuery → MATCH
        OR (
            :booleanQuery IS NOT NULL AND (
                CASE 
                    WHEN :mode = 'TITLE' THEN MATCH(o.title) AGAINST (:booleanQuery IN BOOLEAN MODE)
                    WHEN :mode = 'DESCRIPTION' THEN MATCH(o.message) AGAINST (:booleanQuery IN BOOLEAN MODE)
                    ELSE MATCH(o.title, o.message) AGAINST (:booleanQuery IN BOOLEAN MODE)
                END
            )
        )

        -- 3) Если есть likeQuery → LIKE
        OR (
            :likeQuery IS NOT NULL AND (
                o.title LIKE CONCAT('%', :likeQuery, '%')
                OR o.message LIKE CONCAT('%', :likeQuery, '%')
            )
        )
      )
""", nativeQuery = true)
    long countOrders(
            @Param("sites") List<Long> siteIds,
            @Param("mode") String mode,
            @Param("booleanQuery") String booleanQuery,
            @Param("likeQuery") String likeQuery
    );


}
