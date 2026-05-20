package by.gdev.alert.job.core.service.change.dto;

import java.util.List;

public record SiteInfo(
        String siteName,
        List<ModuleInfo> modules
) {}