package by.gdev.alert.job.parser.domain.truelancer;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class TreuelancerProjects {
    @JsonProperty("data")
    private List<TruelancerOrder> orders;
}
