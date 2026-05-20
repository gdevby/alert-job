package by.gdev.alert.job.parser.service.category.update.dto.changes;

import java.util.List;

public record CategoryUpdateSummary(
        long startTime,
        long endTime,
        long duration,
        int attempts,
        List<CategoryChangeDTO> finalChanges
) {}


