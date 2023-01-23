package by.gdev.alert.job.parser.domain.db;

import javax.persistence.Entity;

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
}
