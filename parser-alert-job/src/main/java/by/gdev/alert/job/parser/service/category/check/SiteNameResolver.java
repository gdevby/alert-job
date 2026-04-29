package by.gdev.alert.job.parser.service.category.check;

import by.gdev.alert.job.parser.util.SiteName;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SiteNameResolver {

    public SiteName resolve(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Site name is null");
        }

        // генерируем ВСЕ возможные варианты
        List<String> candidates = generateCandidates(raw);

        // нормализуем кандидатов
        List<String> normalizedCandidates = candidates.stream()
                .map(this::normalize)
                .distinct()
                .toList();

        // перебираем все enum
        for (SiteName name : SiteName.values()) {
            String normalizedEnum = normalize(name.name());

            for (String candidate : normalizedCandidates) {
                if (candidate.equals(normalizedEnum)) {
                    return name;
                }
            }
        }

        throw new IllegalArgumentException("Unknown site name: " + raw);
    }

    private List<String> generateCandidates(String raw) {
        String upper = raw.toUpperCase();

        int dot = upper.indexOf('.');

        if (dot == -1) {
            // нет точки — возвращаем только исходное
            return List.of(upper);
        }

        String before = upper.substring(0, dot);
        String after = upper.substring(dot + 1);

        return List.of(
                upper,              // FREELANCE.RU
                before,             // FREELANCE
                before + after      // FREELANCERU
        );
    }

    private String normalize(String s) {
        return s
                .trim()
                .toUpperCase()
                .replaceAll("[^A-Z0-9]", "");
    }
}


