package by.gdev.alert.job.parser.domain.parsing;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FreelancerResult {
	@JsonProperty("result")
	private Set<FreelancerSubCategories> categories;
}