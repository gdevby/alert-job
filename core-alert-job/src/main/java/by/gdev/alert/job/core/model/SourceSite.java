package by.gdev.alert.job.core.model;

import lombok.Data;

@Data
public class SourceSite {
	private Long source;
	private Long category;
	private Long subCategory;
	private boolean flRuForAll;
}
