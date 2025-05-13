package by.gdev.alert.job.core.model.db;

import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;

import by.gdev.alert.job.core.model.db.key.DescriptionWord;
import by.gdev.alert.job.core.model.db.key.DescriptionWordPrice;
import by.gdev.alert.job.core.model.db.key.TitleWord;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Data
@EqualsAndHashCode(callSuper = true, exclude = { "titles", "descriptions", "negativeTitles", "negativeDescriptions" })
@ToString(callSuper = true, exclude = { "titles", "descriptions", "negativeTitles", "negativeDescriptions" })
public class UserFilter extends BasicId {

	private String name;
	private Integer minValue;
	private Integer maxValue;
	private boolean openForAll = false;

	@ManyToMany
	private Set<DescriptionWordPrice> descriptionWordPrice;

	@ManyToOne
	private OrderModules module;
	@ManyToMany
	private Set<TitleWord> titles;
	@ManyToMany
	private Set<DescriptionWord> descriptions;

	private boolean activatedNegativeFilters = false;
	@ManyToMany
	private Set<TitleWord> negativeTitles;
	@ManyToMany
	private Set<DescriptionWord> negativeDescriptions;
}