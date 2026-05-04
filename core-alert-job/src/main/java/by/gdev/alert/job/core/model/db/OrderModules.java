package by.gdev.alert.job.core.model.db;

import java.util.Set;

import jakarta.persistence.*;

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
    @Column(name = "auto_reply_enabled")
    private Boolean autoReplyEnabled = true;
    @ManyToOne
	private AppUser user;
	@ManyToMany
	private Set<SourceSite> sources;
	@OneToMany(mappedBy = "module", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<UserFilter> filters;
	@ManyToOne
	private UserFilter currentFilter;
	
}
