package by.gdev.alert.job.parser.domain.parsing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KworkCategoryNode(
        @JsonProperty("CATID") String catId,
        String id,
        String name,
        String parent,
        @JsonProperty("sort_index") String sortIndex,
        List<KworkCategoryNode> cats
) {
}
