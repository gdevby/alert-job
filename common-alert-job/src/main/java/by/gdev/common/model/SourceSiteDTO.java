package by.gdev.common.model;

import lombok.Data;

@Data
public class SourceSiteDTO {
	private Long id;
	private Long source;
	private Long category;
	private Long subCategory;
	private boolean flRuForAll;

}
