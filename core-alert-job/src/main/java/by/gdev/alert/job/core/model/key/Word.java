package by.gdev.alert.job.core.model.key;

import lombok.Data;

//@MappedSuperclass
@Data
public abstract class Word {
	private String name;
	private Long counter;

}
