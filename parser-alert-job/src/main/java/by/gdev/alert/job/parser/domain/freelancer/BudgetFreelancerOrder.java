package by.gdev.alert.job.parser.domain.freelancer;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BudgetFreelancerOrder {
	private Long minimum;
	private Long maximum;
}
