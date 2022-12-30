package by.gdev.alert.job.core.exeption;

import java.util.List;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolationException;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class ErrorHandlingController {
	
	@ResponseStatus(value = HttpStatus.BAD_REQUEST)
	@ExceptionHandler(ConstraintViolationException.class)
	@ResponseBody
	public ValidationErrorResponse resourceNotFoundExceptionHandler(ConstraintViolationException ex) {
		List<Violation> l = ex.getConstraintViolations().stream().map(e -> {
			return new Violation(e.getPropertyPath().toString(), e.getMessageTemplate());
		}).collect(Collectors.toList());
		ValidationErrorResponse v = new ValidationErrorResponse();
		v.setViolations(l);
		return v;
	}
}