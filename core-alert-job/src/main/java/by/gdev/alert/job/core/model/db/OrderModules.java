package by.gdev.alert.job.core.model.db;

import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true, exclude = {"filters", "sources", "currentFilter"})
@ToString(callSuper = true, exclude = {"filters", "sources", "currentFilter"})
@Entity
public class OrderModules extends BasicId {
	
	private String name;
	private boolean available;
	@ManyToOne
	private AppUser user;
	@ManyToMany
	private Set<SourceSite> sources;
	@OneToMany(mappedBy = "module", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<UserFilter> filters;
	@ManyToOne
	private UserFilter currentFilter;
	
}
