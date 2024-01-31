package by.gdev.alert.job.parser.domain.currency;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CurrencyRoot {

	@JsonProperty("Date")
	private String date;
	@JsonProperty("PreviousDate")
	private String previousDate;
	@JsonProperty("PreviousURL")
	private String previousURL;
	@JsonProperty("Timestamp")
	private String timestamp;
	@JsonProperty("Valute")
	private Map<Currency, Valute> valute;
}
