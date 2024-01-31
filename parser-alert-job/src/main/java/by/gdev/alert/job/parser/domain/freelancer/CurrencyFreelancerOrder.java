package by.gdev.alert.job.parser.domain.freelancer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CurrencyFreelancerOrder {

    @JsonProperty("code")
    private String code;
    @JsonProperty("sign")
    private String sign;
    @JsonProperty("name")
    private String name;
    @JsonProperty("exchange_rate")
    private String exchangeRate;
    @JsonProperty("country")
    private String country;
}
