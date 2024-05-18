package by.gdev.alert.job.parser.domain.kwork;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Kwork {

	private String id;
	private String lang;
	private String name;
	private String description;
	private int possiblePriceLimit;
	@JsonProperty("date_create")
	private String dateCreate;
}
