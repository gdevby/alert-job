package by.gdev.alert.job.parser.domain.db;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity(name = "parser_category")
@EqualsAndHashCode(callSuper =  true)
public class Category extends BasicId {

	private String name;
	private String nativeLocName;
	private String link;
	private boolean parse;
	@ManyToOne(cascade = CascadeType.ALL , fetch = FetchType.LAZY)
	private SiteSourceJob siteSourceJob;
	@OrderColumn(name = "POSITION")
	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "category")
	private List<Subcategory> subCategories;
}
