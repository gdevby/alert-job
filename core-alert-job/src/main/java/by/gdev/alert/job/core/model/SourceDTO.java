package by.gdev.alert.job.core.model;

import by.gdev.common.model.CategoryDTO;
import by.gdev.common.model.SiteSourceDTO;
import by.gdev.common.model.SubCategoryDTO;
import lombok.Data;

@Data
public class SourceDTO {

	private Long id;
	private SiteSourceDTO siteSourceDTO;
	private CategoryDTO siteCategoryDTO;
	private SubCategoryDTO siteSubCategoryDTO;
	private boolean openForAll;
}
