package by.gdev.alert.job.core.service.change.dto;

import java.util.List;

public record ModuleInfo(
        String moduleName,
        List<RemovedCategoryInfo> removed
) {}
