package by.gdev.alert.job.core.model.db;

import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Data
@EqualsAndHashCode(callSuper = true, exclude = { "orderModules", "userAlertTimes", "delayOrderNotifications" })
@ToString(callSuper = true, exclude = { "orderModules", "userAlertTimes", "delayOrderNotifications" })

public class AppUser extends BasicId {

    private String uuid;
    @OneToMany(mappedBy = "user")
    private Set<OrderModules> orderModules;
    @OneToMany(mappedBy = "user")
    private Set<UserAlertTime> userAlertTimes;
    private String email;
    private Long telegram;
    private boolean switchOffAlerts = true;
    private boolean defaultSendType = true;
    @OneToMany(mappedBy = "user")
    private Set<DelayOrderNotification> delayOrderNotifications;
    private Integer telegramFailCount = 0;
}
