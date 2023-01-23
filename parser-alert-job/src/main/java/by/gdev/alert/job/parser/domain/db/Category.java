package by.gdev.alert.job.parser.domain.db;

import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;

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
	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	private Set<Subcategory> subCategories;
}
