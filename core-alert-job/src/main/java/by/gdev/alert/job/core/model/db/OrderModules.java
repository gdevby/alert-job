package by.gdev.alert.job.core.model.db;

import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.validation.constraints.Size;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true, exclude = {"filters", "sources"})
@ToString(callSuper = true, exclude = {"filters", "sources"})
@Entity
public class OrderModules extends BasicId {
	
	private String name;
	private boolean available;
	@ManyToOne(fetch = FetchType.LAZY)
	private AppUser user;
	@ManyToMany
	private Set<SourceSite> sources;
	@OneToMany(mappedBy = "module", cascade = CascadeType.ALL, orphanRemoval = true )
	@Size(max = 5, message = "the limit for added filters")
	private Set<UserFilter> filters;
	@ManyToOne(fetch = FetchType.LAZY)
	private UserFilter currentFilter;
	
}
