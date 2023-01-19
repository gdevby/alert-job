package by.gdev.alert.job.parser.domain.db;

import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@EqualsAndHashCode(callSuper = true)
public class SiteSourceJob extends BasicId {

	private String name;
	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	private Set<Category> categories;
	private String parsedURI;
	private boolean parse;

}
