package by.gdev.alert.job.parser.domain.parsing;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FreelancerSubCategories {

	@JsonProperty("id")
	private Integer id;
	@JsonProperty("name")
	private String name;
	@JsonProperty("category")
	private FreelancerCategories category;
	@JsonProperty("seo_url")
	private String seoUrl;
}
