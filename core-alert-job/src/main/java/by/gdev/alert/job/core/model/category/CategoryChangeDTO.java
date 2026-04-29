package by.gdev.alert.job.core.model.category;

public record CategoryChangeDTO(
        String siteName,
        CategoryDiffResult diff
) {}
