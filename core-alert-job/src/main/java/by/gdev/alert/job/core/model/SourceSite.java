package by.gdev.alert.job.core.model;

import lombok.Data;

@Data
public class SourceSite {
	private Long siteSource;
	private Long siteCategory;
	private Long siteSubCategory;
	private boolean flRuForAll;
}
