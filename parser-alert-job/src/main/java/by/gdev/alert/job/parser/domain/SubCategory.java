package by.gdev.alert.job.parser.domain;

import javax.persistence.Entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@EqualsAndHashCode(callSuper = true)
public class SubCategory extends BasicId {
	
	private String name;
}
