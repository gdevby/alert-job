package by.gdev.alert.job.core.model;

import java.util.List;

import lombok.Data;

@Data
public class FilterDTO {
	
	private Long id;
	private String name;
	private Integer minValue;
	private Integer maxValue;
	private boolean openForAll;
	
	private List<WordDTO> descriptionWordPrice;
	private List<WordDTO> titlesDTO;
	private List<WordDTO> descriptionsDTO;
	
	private boolean activatedNegativeFilters;
	private List<WordDTO> negativeTitlesDTO;
	private List<WordDTO> negativeDescriptionsDTO;
}
