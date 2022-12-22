package by.gdev.alert.job.core.model;

import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;

import by.gdev.alert.job.core.model.key.DescriptionWord;
import by.gdev.alert.job.core.model.key.TechnologyWord;
import by.gdev.alert.job.core.model.key.TitleWord;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
public class UserFilter extends BasicId {
	private String name;
	private Integer minValue;
	private Integer maxValue;
	@ManyToMany
	private Set<TechnologyWord> technologies;
	@ManyToMany
	private Set<TitleWord> titles;
	@ManyToMany
	private Set<DescriptionWord> descriptions;
}
