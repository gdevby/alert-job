package by.gdev.alert.job.core.model;

import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;

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
	private Set<UserFilter> filters;
	@ManyToOne
	private UserFilter currentFilter;
	private String email;
	private Integer telegram;
	private boolean switchOffAlerts;
}
