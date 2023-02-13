package by.gdev.alert.job.core.model;

import java.util.List;

import lombok.Data;

@Data
public class OrderModulesDTO {

	private Long id;
	private String name;
	private List<SourceDTO> sourceDTO;
	private List<FilterDTO> filterDTO;
	private FilterDTO currentFilter;
}
