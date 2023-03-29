package by.gdev.alert.job.core.model.db;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@EqualsAndHashCode(callSuper = true)
public class UserAlertTime extends BasicId {
    
    @Column(nullable = false)
    private Integer alertDate;
    @Column(nullable = false)
    private Integer startAlert;
    @Column(nullable = false)
    private Integer endAlert;
    @Column(nullable = false)
    private String timeZone;
    @ManyToOne
    private AppUser user;
}
