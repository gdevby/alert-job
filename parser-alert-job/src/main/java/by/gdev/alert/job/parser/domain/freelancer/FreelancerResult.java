package by.gdev.alert.job.parser.domain.freelancer;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class FreelancerResult {

	@JsonProperty("projects")
	private List<FreelancerProject> projects;

}
