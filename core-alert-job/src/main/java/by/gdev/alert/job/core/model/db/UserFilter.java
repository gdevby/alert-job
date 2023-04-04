package by.gdev.alert.job.core.model.db;

import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;

import by.gdev.alert.job.core.model.db.key.DescriptionWord;
import by.gdev.alert.job.core.model.db.key.DescriptionWordPrice;
import by.gdev.alert.job.core.model.db.key.TechnologyWord;
import by.gdev.alert.job.core.model.db.key.TitleWord;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Data
@EqualsAndHashCode(callSuper = true, exclude = { "technologies", "titles", "descriptions", "negativeTechnologies",
	"negativeTitles", "negativeDescriptions" })
@ToString(callSuper = true, exclude = { "technologies", "titles", "descriptions", "negativeTechnologies",
	"negativeTitles", "negativeDescriptions" })
public class UserFilter extends BasicId {

    private String name;
    private Integer minValue;
    private Integer maxValue;
    private boolean openForAll = false;

    @ManyToMany(fetch = FetchType.EAGER)
    private Set<DescriptionWordPrice> descriptionWordPrice;

    @ManyToOne
    private OrderModules module;
    @ManyToMany(fetch = FetchType.EAGER)
    private Set<TechnologyWord> technologies;
    @ManyToMany(fetch = FetchType.EAGER)
    private Set<TitleWord> titles;
    @ManyToMany(fetch = FetchType.EAGER)
    private Set<DescriptionWord> descriptions;

    private boolean activatedNegativeFilters = false;
    @ManyToMany(fetch = FetchType.EAGER)
    private Set<TechnologyWord> negativeTechnologies;
    @ManyToMany(fetch = FetchType.EAGER)
    private Set<TitleWord> negativeTitles;
    @ManyToMany(fetch = FetchType.EAGER)
    private Set<DescriptionWord> negativeDescriptions;
}