package by.gdev.alert.job.parser.domain.db;

import java.util.Set;

import jakarta.persistence.*;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SiteSourceJob extends BasicId {

	private String name;
	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "siteSourceJob")
	private Set<Category> categories;
	private String parsedURI;
	private boolean parse;
	private boolean active;
}
