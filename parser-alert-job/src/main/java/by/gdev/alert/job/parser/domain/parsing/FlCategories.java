package by.gdev.alert.job.parser.domain.parsing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FlCategories(List<FlCategoryItem> items) {}
