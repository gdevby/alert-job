package by.gdev.alert.job.core.model.db;

import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Data
@EqualsAndHashCode(callSuper = true, exclude = "orderModules")
@ToString(callSuper = true, exclude = "orderModules")

public class AppUser extends BasicId{

	private String uuid;
	@OneToMany(mappedBy = "user")
	private Set<OrderModules> orderModules;
	private String email;
	private Long telegram;
	private boolean switchOffAlerts = true;
	private boolean defaultSendType = true;
}
