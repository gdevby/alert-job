package by.gdev.alert.job.core.model;

import java.util.Set;

import by.gdev.alert.job.core.model.key.DescriptionWord;
import by.gdev.alert.job.core.model.key.TechnologyWord;
import by.gdev.alert.job.core.model.key.TitleWord;
import lombok.Data;
@Data
public class UserFilter {
	private String name;
	private Integer minValue;
	private Integer maxValue;
	private Set<TechnologyWord> technologies;
	private Set<TitleWord> titles;
	private Set<DescriptionWord> descriptions;
}
