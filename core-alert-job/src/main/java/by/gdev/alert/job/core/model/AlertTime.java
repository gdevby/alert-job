package by.gdev.alert.job.core.model;

import lombok.Data;

@Data
public class AlertTime {

	private Long alertDate;
    private Long startAlert;
    private Long endAlert;
    private String timeZone;
}