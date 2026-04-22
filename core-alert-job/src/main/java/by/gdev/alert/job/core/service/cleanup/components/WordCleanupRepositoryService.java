package by.gdev.alert.job.core.service.cleanup.components;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class WordCleanupRepositoryService {
    private final JdbcTemplate jdbc;

    public List<Long> getTitleWordIds(Long sourceSiteId) {
        return jdbc.query(
                "SELECT id FROM title_word WHERE source_site_id = ?",
                (rs, rowNum) -> rs.getLong("id"),
                sourceSiteId
        );
    }

    public Map<Long, String> getWordsByIds(List<Long> ids) {
        if (ids.isEmpty()) return Map.of();

        String inSql = ids.stream().map(id -> "?").collect(Collectors.joining(","));

        return jdbc.query(
                "SELECT id, name FROM title_word WHERE id IN (" + inSql + ")",
                rs -> {
                    Map<Long, String> map = new HashMap<>();
                    while (rs.next()) {
                        map.put(rs.getLong("id"), rs.getString("name"));
                    }
                    return map;
                },
                ids.toArray()
        );
    }

    public List<Long> getPositiveWordsForModule(Long moduleId, List<Long> deletedWordIds) {

        if (deletedWordIds.isEmpty()) return List.of();

        String inSql = deletedWordIds.stream().map(id -> "?").collect(Collectors.joining(","));

        String sql = """
            SELECT uft.titles_id
            FROM user_filter_titles uft
            JOIN user_filter uf ON uf.id = uft.user_filter_id
            WHERE uf.module_id = ?
              AND uft.titles_id IN (%s)
        """.formatted(inSql);

        Object[] params = Stream.concat(Stream.of(moduleId), deletedWordIds.stream()).toArray();

        return jdbc.query(sql, (rs, rowNum) -> rs.getLong("titles_id"), params);
    }

    public List<Long> getNegativeWordsForModule(Long moduleId, List<Long> deletedWordIds) {

        if (deletedWordIds.isEmpty()) return List.of();

        String inSql = deletedWordIds.stream().map(id -> "?").collect(Collectors.joining(","));

        String sql = """
            SELECT ufnt.negative_titles_id
            FROM user_filter_negative_titles ufnt
            JOIN user_filter uf ON uf.id = ufnt.user_filter_id
            WHERE uf.module_id = ?
              AND ufnt.negative_titles_id IN (%s)
        """.formatted(inSql);

        Object[] params = Stream.concat(Stream.of(moduleId), deletedWordIds.stream()).toArray();

        return jdbc.query(sql, (rs, rowNum) -> rs.getLong("negative_titles_id"), params);
    }
}
