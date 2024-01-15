package by.gdev.alert.job.parser.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import lombok.Data;

@Data
@JacksonXmlRootElement(localName = "Root")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class YouDoRoot {

	@JsonProperty("ResultObject")
	private ResultObject resultObject;
	@JsonProperty("IsSuccess")
	private boolean isSuccess;
	@JsonProperty("Code")
	private int code;
	@JsonProperty("Message")
	private String message;
	@JsonProperty("Timestamp")
	private String timestamp;
}
