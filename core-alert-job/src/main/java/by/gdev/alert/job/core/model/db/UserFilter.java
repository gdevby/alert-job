package by.gdev.alert.job.core.model.db;

import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.validation.constraints.Size;

import by.gdev.alert.job.core.model.db.key.DescriptionWord;
import by.gdev.alert.job.core.model.db.key.TechnologyWord;
import by.gdev.alert.job.core.model.db.key.TitleWord;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Data
@EqualsAndHashCode(callSuper = true, exclude = { "technologies", "titles", "descriptions", "negativeTechnologies",
		"negativeTitles", "negativeDescriptions"})
@ToString(callSuper = true, exclude = { "technologies", "titles", "descriptions", "negativeTechnologies",
		"negativeTitles", "negativeDescriptions"})
public class UserFilter extends BasicId {

	private String name;
	private Integer minValue;
	private Integer maxValue;
	@ManyToOne
	private OrderModules module;
	@ManyToMany(fetch = FetchType.EAGER)
	@Size(max = 50, message = "the limit for added technology word")
	private Set<TechnologyWord> technologies;
	@ManyToMany(fetch = FetchType.EAGER)
	@Size(max = 50, message = "the limit for added title word")
	private Set<TitleWord> titles;
	@ManyToMany(fetch = FetchType.EAGER)
	@Size(max = 50, message = "the limit for added description word")
	private Set<DescriptionWord> descriptions;

	private boolean activatedNegativeFilters = false;
	@ManyToMany(fetch = FetchType.EAGER)
	@Size(max = 50, message = "the limit for added negative technology word")
	private Set<TechnologyWord> negativeTechnologies;
	@ManyToMany(fetch = FetchType.EAGER)
	@Size(max = 50, message = "the limit for added negative title word")
	private Set<TitleWord> negativeTitles;
	@ManyToMany(fetch = FetchType.EAGER)
	@Size(max = 50, message = "the limit for added negative description word")
	private Set<DescriptionWord> negativeDescriptions;
}