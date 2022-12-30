package by.gdev.alert.job.core.exeption;

import lombok.Data;

@Data
public class Violation {
	private final String fieldName;
	private final String message;
}
