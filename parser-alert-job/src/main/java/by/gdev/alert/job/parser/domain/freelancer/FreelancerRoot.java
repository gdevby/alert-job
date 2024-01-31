package by.gdev.alert.job.parser.domain.freelancer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FreelancerRoot {

	@JsonProperty("status")
	private String status;
	@JsonProperty("result")
	private FreelancerResult result;
	@JsonProperty("request_id")
	private String requestId;

}
