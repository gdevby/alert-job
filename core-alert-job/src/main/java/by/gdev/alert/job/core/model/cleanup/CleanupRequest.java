package by.gdev.alert.job.core.model.cleanup;

import java.util.List;

public record CleanupRequest(
        Long siteId,
        String siteName,
        List<ParserCategoryDTO> categories
) {}

