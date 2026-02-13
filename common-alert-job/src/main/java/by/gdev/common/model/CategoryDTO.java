package by.gdev.common.model;

import lombok.Data;

import java.util.List;

@Data
public class CategoryDTO {

	private Long id;
	private String name;
	private String nativeLocName;
	private String link;
	private boolean parse;
}
