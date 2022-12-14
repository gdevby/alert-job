package by.gdev.alert.job.parser.domain;

import javax.persistence.Entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@EqualsAndHashCode(callSuper =  true)
public class Category extends BasicId {

	private String name;
}
