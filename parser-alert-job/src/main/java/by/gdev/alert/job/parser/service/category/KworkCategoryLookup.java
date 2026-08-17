package by.gdev.alert.job.parser.service.category;

import by.gdev.alert.job.parser.domain.parsing.KworkCategoryNode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
final class KworkCategoryLookup {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final Map<String, Long> topLevelByName = new HashMap<>();
    private final Map<String, Map<String, Long>> subByParentAndName = new HashMap<>();

    static KworkCategoryLookup fromPageHtml(String html) {
        KworkCategoryLookup lookup = new KworkCategoryLookup();
        if (html == null || html.isBlank()) {
            return lookup;
        }
        try {
            String categoriesJson = KworkCategoryParser.extractJsonObject(html, "categories");
            if (categoriesJson == null) {
                log.warn("categories JSON не найден в HTML kwork.ru");
                return lookup;
            }
            Map<String, KworkCategoryNode> categories = OBJECT_MAPPER.readValue(
                    categoriesJson,
                    new TypeReference<Map<String, KworkCategoryNode>>() {}
            );
            lookup.fillFromCategories(categories);
        } catch (Exception e) {
            log.warn("Не удалось построить lookup категорий kwork.ru из JSON", e);
        }
        return lookup;
    }

    private void fillFromCategories(Map<String, KworkCategoryNode> categories) {
        for (KworkCategoryNode node : categories.values()) {
            String nodeId = resolveId(node);
            if (nodeId == null || node.name() == null) {
                continue;
            }
            if ("0".equals(node.parent())) {
                topLevelByName.put(normalizeName(node.name()), Long.parseLong(nodeId));
            } else if (node.parent() != null) {
                registerSub(node.parent(), node.name(), nodeId);
            }
            if (node.cats() != null) {
                for (KworkCategoryNode sub : node.cats()) {
                    String subId = resolveId(sub);
                    if (subId != null && sub.name() != null) {
                        registerSub(nodeId, sub.name(), subId);
                    }
                }
            }
        }
    }

    private void registerSub(String parentId, String name, String subId) {
        subByParentAndName
                .computeIfAbsent(parentId, key -> new HashMap<>())
                .put(normalizeName(name), Long.parseLong(subId));
    }

    Long resolveTopId(String name) {
        return topLevelByName.get(normalizeName(name));
    }

    Long resolveSubId(String parentId, String subName) {
        if (parentId == null) {
            return null;
        }
        Map<String, Long> subs = subByParentAndName.get(parentId);
        if (subs == null) {
            return null;
        }
        return subs.get(normalizeName(subName));
    }

    private static String resolveId(KworkCategoryNode node) {
        if (node.catId() != null && !node.catId().isBlank()) {
            return node.catId();
        }
        return node.id();
    }

    private static String normalizeName(String name) {
        return name.replaceAll("\\s+", " ").trim();
    }
}
