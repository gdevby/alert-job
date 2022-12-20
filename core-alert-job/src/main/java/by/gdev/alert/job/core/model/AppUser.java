package by.gdev.alert.job.core.model;

import java.util.Set;

import lombok.Data;
@Data
public class AppUser {
	private String uuid;
	private Set<SourceSite> sources;
	private Set<UserFilter> filters;
	private UserFilter currentFilter;
	private String email;
	private Integer telegram;
	private boolean switchOffAlerts;
}
