package by.gdev.alert.job.parser.domain.workana;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkanaRoot {
    @JsonProperty("results")
    private WorkanaOrderWrap workanaOrderWrap;
}


