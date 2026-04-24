package by.gdev.alert.job.parser.domain;

import by.gdev.alert.job.parser.service.category.cleanup.CleanupMode;

import java.util.List;

public record CleanupRequest(
        Long siteId,
        String siteName,
        List<ParserCategoryDTO> categories,
        CleanupMode mode
) {}
