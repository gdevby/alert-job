package by.gdev.alert.job.core.model.key;

import javax.persistence.MappedSuperclass;

import by.gdev.alert.job.core.model.BasicId;
import lombok.Data;
import lombok.EqualsAndHashCode;

@MappedSuperclass
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class Word extends BasicId {
	private String name;
	private Long counter;

}
