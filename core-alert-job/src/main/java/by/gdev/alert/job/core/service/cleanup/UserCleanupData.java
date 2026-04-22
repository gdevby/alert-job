package by.gdev.alert.job.core.service.cleanup;

import by.gdev.alert.job.core.model.db.AppUser;
import lombok.Getter;

import java.util.List;

@Getter
public class UserCleanupData {
    private AppUser user;
    private List<UserModuleCleanupData> modules;

    public UserCleanupData(AppUser user, List<UserModuleCleanupData> modules) {
        this.user = user;
        this.modules = modules;
    }
}
