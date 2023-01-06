package by.gdev.alert.job.core.model.db;

import javax.persistence.Entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
public class SourceSite extends BasicId{
	private Long siteSource;
	private Long siteCategory;
	private Long siteSubCategory;
	private boolean flRuForAll;
}
