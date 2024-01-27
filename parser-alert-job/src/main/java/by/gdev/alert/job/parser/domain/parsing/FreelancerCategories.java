package by.gdev.alert.job.parser.domain.parsing;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FreelancerCategories {
    @JsonProperty("id")
    private Integer id;
    @JsonProperty("name")
    private String name;
}