package by.gdev.alert.job.parser.domain.db;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;

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
	private boolean active;
}
