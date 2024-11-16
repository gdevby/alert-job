package by.gdev.alert.job.parser.domain.workana;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkanaOrderWrap {

    @JsonProperty("results")
    private List<WorkanaOrder> workanaOrders;

}