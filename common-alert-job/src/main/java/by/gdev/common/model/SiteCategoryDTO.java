package by.gdev.common.model;

import lombok.Data;

@Data
public class SiteCategoryDTO {
	
	private Long id;
	private CategoryDTO category;
	private String link;
	private boolean parse;
}
