package by.gdev.alert.job.parser.domain.truelancer;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class TrueLancerCategory {
    private String category;
    @JsonProperty("sub_cats")
    private Map<String, String > subCategories;
}
