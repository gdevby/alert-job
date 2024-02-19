package by.gdev.alert.job.parser.domain.kwork;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Kwork {
	private String lang;
	private String name;
	private String description;
	private String url;
	private int possiblePriceLimit;
	private String dateCreate;
}
