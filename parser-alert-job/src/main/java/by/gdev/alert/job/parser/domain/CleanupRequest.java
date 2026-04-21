package by.gdev.alert.job.parser.domain;

import java.util.List;

public record CleanupRequest(
        Long siteId,
        String siteName,
        List<ParserCategoryDTO> categories
) {}

