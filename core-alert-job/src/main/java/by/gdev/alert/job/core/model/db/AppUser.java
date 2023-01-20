package by.gdev.alert.job.core.model.db;

import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.validation.constraints.Size;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Data
@EqualsAndHashCode(callSuper = true, exclude = {"sources", "filters", "currentFilter"})
@ToString(exclude = {"sources", "filters", "currentFilter"})
public class AppUser extends BasicId{
	private String uuid;
	@ManyToMany(fetch = FetchType.LAZY)
	private Set<SourceSite> sources;
	@OneToMany(fetch = FetchType.LAZY)
	@Size(max = 5, message = "the limit for added filters")
	private Set<UserFilter> filters;
	@ManyToOne(fetch = FetchType.LAZY)
	private UserFilter currentFilter;
	private String email;
	private Long telegram;
	private boolean switchOffAlerts = true;
	private boolean defaultSendType = true;
}
