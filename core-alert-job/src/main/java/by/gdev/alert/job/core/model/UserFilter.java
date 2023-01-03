package by.gdev.alert.job.core.model;

import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.validation.constraints.Size;

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
	@Size(max = 50, message = "the limit for added technology word")
	private Set<TechnologyWord> technologies;
	@ManyToMany
	@Size(max = 50, message = "the limit for added title word")
	private Set<TitleWord> titles;
	@ManyToMany
	@Size(max = 50, message = "the limit for added description word")
	private Set<DescriptionWord> descriptions;
}
