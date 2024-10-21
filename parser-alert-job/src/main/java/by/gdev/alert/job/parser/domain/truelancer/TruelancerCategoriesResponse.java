package by.gdev.alert.job.parser.domain.truelancer;

import lombok.Data;

import java.util.Map;
@Data
public class TruelancerCategoriesResponse {
    private int status;
    private Map<String, TrueLancerCategory> categories;
}