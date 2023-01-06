package by.gdev.common.model;

import lombok.Data;

@Data
public class SourceSiteDTO {
	private Long id;
	private Long siteSource;
	private Long siteCategory;
	private Long siteSubCategory;
	private boolean flRuForAll;

}
