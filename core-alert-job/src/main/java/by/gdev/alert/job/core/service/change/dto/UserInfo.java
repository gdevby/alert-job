package by.gdev.alert.job.core.service.change.dto;

import by.gdev.alert.job.core.model.db.AppUser;

import java.util.List;

public record UserInfo(
        AppUser user,
        List<SiteInfo> sites
) {}
