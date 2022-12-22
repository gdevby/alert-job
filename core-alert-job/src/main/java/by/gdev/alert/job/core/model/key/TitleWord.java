package by.gdev.alert.job.core.model.key;

import javax.persistence.Entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
public class TitleWord extends Word {
	private String hide;
}
