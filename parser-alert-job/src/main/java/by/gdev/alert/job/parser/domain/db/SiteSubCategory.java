package by.gdev.alert.job.parser.domain.db;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@EqualsAndHashCode(callSuper = true)
public class SiteSubCategory extends BasicId {
	
	@ManyToOne
	private SubCategory subCategory;
	private String link;
	private boolean parse;
}
