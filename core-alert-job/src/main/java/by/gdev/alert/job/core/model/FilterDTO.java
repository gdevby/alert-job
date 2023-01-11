package by.gdev.alert.job.core.model;

import java.util.List;

import lombok.Data;

@Data
public class FilterDTO {
	
	private Long id;
	private String name;
	private Integer minValue;
	private Integer maxValue;
	private List<WordDTO> technologiesDTO;
	private List<WordDTO> titlesDTO;
	private List<WordDTO> descriptionsDTO;
	
}
