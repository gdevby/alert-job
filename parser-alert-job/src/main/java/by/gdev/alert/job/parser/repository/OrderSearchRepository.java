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
        WHERE ps.source = :siteId
          AND (:categoryId IS NULL OR ps.category = :categoryId)
          AND (:subCategoryId IS NULL OR ps.sub_category = :subCategoryId)
          AND (
                :keywords IS NULL
                OR MATCH(o.title, o.message) AGAINST (:keywords IN BOOLEAN MODE)
              )
        ORDER BY o.date_time DESC
        LIMIT :offset, :size
        """,
            nativeQuery = true)
    List<Order> searchOrders(
            @Param("siteId") Long siteId,
            @Param("categoryId") Long categoryId,
            @Param("subCategoryId") Long subCategoryId,
            @Param("keywords") String keywords,
            @Param("offset") int offset,
            @Param("size") int size
    );


    @Query(value = """
    SELECT COUNT(*)
    FROM parser_order o
    JOIN parser_order_source ps ON ps.id = o.source_site_id
    WHERE ps.source = :siteId
      AND (:categoryId IS NULL OR ps.category = :categoryId)
      AND (:subCategoryId IS NULL OR ps.sub_category = :subCategoryId)
      AND (:keywords IS NULL OR MATCH(o.title, o.message) AGAINST (:keywords IN BOOLEAN MODE))
    """,
            nativeQuery = true)
    long countOrders(
            @Param("siteId") Long siteId,
            @Param("categoryId") Long categoryId,
            @Param("subCategoryId") Long subCategoryId,
            @Param("keywords") String keywords
    );

}
