package by.gdev.alert.job.parser.domain;

import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@EqualsAndHashCode(callSuper =  true)
public class SiteSourceJob extends BasicId {
	
	private String name;
	@ManyToMany
	private Set<SiteCategory> siteCategories;
	

}
