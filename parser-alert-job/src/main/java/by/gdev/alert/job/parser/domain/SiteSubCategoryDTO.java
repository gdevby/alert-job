package by.gdev.alert.job.parser.domain;

import lombok.Data;

@Data
public class SiteSubCategoryDTO {
	
	private Long id;
	private SubCategoryDTO subCategory;
	private String link;
	private boolean parse;

}
