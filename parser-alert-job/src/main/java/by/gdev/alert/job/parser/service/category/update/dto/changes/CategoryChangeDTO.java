package by.gdev.alert.job.parser.service.category.update.dto.changes;

public record CategoryChangeDTO(
        Long siteSourceId,
        String siteName,
        CategoryDiffDTO diff
) {}


