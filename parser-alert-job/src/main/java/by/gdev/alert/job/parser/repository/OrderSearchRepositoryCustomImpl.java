package by.gdev.alert.job.parser.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class OrderSearchRepositoryCustomImpl implements OrderSearchRepositoryCustom {

    private final EntityManager em;

    @Override
    public List searchOrdersDynamic(List<Long> siteIds, String mode, List<String> words, int offset, int size) {

        StringBuilder sql = new StringBuilder("""
            SELECT 
                o.id, o.title, o.message, o.link, o.date_time, o.order_price,
                ps.category, ps.sub_category
            FROM parser_order o
            JOIN parser_order_source ps ON ps.id = o.source_site_id
            WHERE ps.id IN :sites
        """);

        if (!words.isEmpty()) {
            sql.append(" AND (");
            for (int i = 0; i < words.size(); i++) {
                if (i > 0) sql.append(" OR ");

                if ("TITLE".equals(mode)) {
                    sql.append(" o.title LIKE :w").append(i);
                } else if ("DESCRIPTION".equals(mode)) {
                    sql.append(" o.message LIKE :w").append(i);
                } else {
                    sql.append("(o.title LIKE :w").append(i)
                            .append(" OR o.message LIKE :w").append(i).append(")");
                }
            }
            sql.append(")");
        }

        sql.append(" ORDER BY o.date_time DESC LIMIT :offset, :size");

        Query q = em.createNativeQuery(sql.toString());

        q.setParameter("sites", siteIds);
        q.setParameter("offset", offset);
        q.setParameter("size", size);

        for (int i = 0; i < words.size(); i++) {
            q.setParameter("w" + i, "%" + words.get(i) + "%");
        }

        return q.getResultList();
    }

    @Override
    public long countOrdersDynamic(List<Long> siteIds, String mode, List<String> words) {

        StringBuilder sql = new StringBuilder("""
            SELECT COUNT(*)
            FROM parser_order o
            JOIN parser_order_source ps ON ps.id = o.source_site_id
            WHERE ps.id IN :sites
        """);

        if (!words.isEmpty()) {
            sql.append(" AND (");
            for (int i = 0; i < words.size(); i++) {
                if (i > 0) sql.append(" OR ");

                if ("TITLE".equals(mode)) {
                    sql.append(" o.title LIKE :w").append(i);
                } else if ("DESCRIPTION".equals(mode)) {
                    sql.append(" o.message LIKE :w").append(i);
                } else {
                    sql.append("(o.title LIKE :w").append(i)
                            .append(" OR o.message LIKE :w").append(i).append(")");
                }
            }
            sql.append(")");
        }

        Query q = em.createNativeQuery(sql.toString());
        q.setParameter("sites", siteIds);

        for (int i = 0; i < words.size(); i++) {
            q.setParameter("w" + i, "%" + words.get(i) + "%");
        }

        return ((Number) q.getSingleResult()).longValue();
    }
}
