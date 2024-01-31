package by.gdev.alert.job.parser.domain.freelancer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FreelancerProject {
	@JsonProperty("id")
	private Long id;
	@JsonProperty("owner_id")
	private Long ownerId;
	@JsonProperty("title")
	private String title;
	@JsonProperty("type")
	private String type;
	@JsonProperty("seo_url")
	private String seoUrl;
	@JsonProperty("currency")
	private CurrencyFreelancerOrder currency;
	@JsonProperty("preview_description")
	private String description;
	@JsonProperty("budget")
	private BudgetFreelancerOrder budget;

}
