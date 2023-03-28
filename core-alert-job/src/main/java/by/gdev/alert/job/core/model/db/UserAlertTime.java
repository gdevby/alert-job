package by.gdev.alert.job.core.model.db;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@EqualsAndHashCode(callSuper = true)
public class UserAlertTime extends BasicId {
    
    private Long alertDate;
    private Long startAlert;
    private Long endAlert;
    @ManyToOne
    private AppUser user;
}
