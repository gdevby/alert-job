package by.gdev.alert.job.parser.domain;

import javax.persistence.Entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity(name = "parser_sub_category")
@EqualsAndHashCode(callSuper = true)
public class SubCategory extends BasicId {
	
	private String name;
}
