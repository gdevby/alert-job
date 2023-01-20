package by.gdev.alert.job.core.model;

import org.hibernate.validator.constraints.Length;

import lombok.Data;

@Data
public class KeyWord {

	@Length(max = 256)
	private String name;
}
