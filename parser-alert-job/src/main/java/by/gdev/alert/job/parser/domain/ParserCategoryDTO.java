package by.gdev.alert.job.parser.domain;

public record ParserCategoryDTO(
        Long categoryId,
        String categoryName,
        Long subCategoryId,
        String subCategoryName
) {}

