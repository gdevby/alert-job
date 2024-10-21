package by.gdev.alert.job.parser.domain.truelancer;

import lombok.Data;

@Data
public class TruelancerOrder {
    private int id;
    private int status;
    private int jobtype;
    private int user_id;
    private String title;
    private String description;
    private int category_id;
    private String created_at;
    private String link;
    private String jobTypeName;
    private boolean alreadyApplied;
    private int budget;
    private String currency;
}
