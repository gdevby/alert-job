package by.gdev.alert.job.core.model;

import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.validation.constraints.Size;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
public class AppUser extends BasicId{
	private String uuid;
	@ManyToMany
	private Set<SourceSite> sources;
	@ManyToMany
	@Size(max = 5, message = "the limit for added filters")
	private Set<UserFilter> filters;
	@ManyToOne
	private UserFilter currentFilter;
	private String email;
	private Integer telegram;
	private boolean switchOffAlerts = true;
	private boolean defaultSendType = true;
}
