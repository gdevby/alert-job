package by.gdev.alert.job.parser.domain.parsing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FlCategoryItem(
        int id,
        String name,
        String name_en,
        Integer rank,
        String link
) {}
