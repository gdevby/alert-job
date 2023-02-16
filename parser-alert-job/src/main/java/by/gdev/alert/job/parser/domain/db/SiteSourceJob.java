package by.gdev.alert.job.parser.domain.db;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@EqualsAndHashCode(callSuper = true)
public class SiteSourceJob extends BasicId {

	private String name;
	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "siteSourceJob")
	@OrderColumn(name = "POSITION")
	private List<Category> categories;
	private String parsedURI;
	private boolean parse;

}
