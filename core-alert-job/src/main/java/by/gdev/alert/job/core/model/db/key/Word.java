package by.gdev.alert.job.core.model.db.key;

import javax.persistence.MappedSuperclass;

import by.gdev.alert.job.core.model.db.BasicId;
import lombok.Data;
import lombok.EqualsAndHashCode;

@MappedSuperclass
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class Word extends BasicId {
	private String name;
	private Long counter = 0L;
	private boolean hidden;

}
