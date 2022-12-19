package by.gdev.alert.job.parser.domain.db;

import javax.persistence.Entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity(name = "parser_categoty")
@EqualsAndHashCode(callSuper =  true)
public class Category extends BasicId {

	private String name;
}
