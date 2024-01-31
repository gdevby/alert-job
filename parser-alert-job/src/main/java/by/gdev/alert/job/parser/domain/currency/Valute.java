package by.gdev.alert.job.parser.domain.currency;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class Valute {
	@JsonProperty("ID")
	private String iD;
	@JsonProperty("NumCode")
	private String numCode;
	@JsonProperty("CharCode")
	private String charCode;
	@JsonProperty("Nominal")
	private int nominal;
	@JsonProperty("Name")
	private String name;
	@JsonProperty("Value")
	private double value;
	@JsonProperty("Previous")
	private double previous;

}
