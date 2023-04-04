package by.gdev.alert.job.core.model;

import javax.validation.constraints.NotNull;

import lombok.Data;

@Data
public class Source {
	@NotNull(message = "source can't be empty")
	private Long siteSource;
	@NotNull(message = "category cant't be empty")
	private Long siteCategory;
	private Long siteSubCategory;
	private boolean openForAll;
}
