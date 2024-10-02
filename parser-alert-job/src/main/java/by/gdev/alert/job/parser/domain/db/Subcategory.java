package by.gdev.alert.job.parser.domain.db;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity(name = "parser_sub_category")
@EqualsAndHashCode(callSuper = true)
public class Subcategory extends BasicId {
	
	private String name;
	private String link;
	private boolean parse;
	private String nativeLocName;
	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private Category category;
}
