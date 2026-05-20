package by.gdev.alert.job.core.model.category;

public record CategoryChangeDTO(
        Long siteSourceId,
        String siteName,
        CategoryDiffDTO diff
) {}
