package by.gdev.common.model;

import lombok.Data;

@Data
public class CategoryDTO {

	private Long id;
	private String name;
	private String nativeLocName;
	private String link;
	private boolean parse;

}
