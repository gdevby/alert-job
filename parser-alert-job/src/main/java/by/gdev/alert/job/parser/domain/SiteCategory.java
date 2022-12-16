package by.gdev.alert.job.parser.domain;

import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@EqualsAndHashCode(callSuper =  true)
public class SiteCategory extends BasicId {
	
	@ManyToOne
	private Category category;
	@OneToMany
	private Set<SiteSubCategory> siteSubCategories;
	private String link;
	private boolean parse;
}
