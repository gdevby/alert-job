package by.gdev.alert.job.core.exeption;

import java.util.List;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import by.gdev.common.exeption.ResourceNotFoundException;

@ControllerAdvice
public class ErrorHandlingController {
	
	@ResponseStatus(value = HttpStatus.BAD_REQUEST)
	@ExceptionHandler(ConstraintViolationException.class)
	@ResponseBody
	public ValidationErrorResponse constraintViolationExceptionHandler(ConstraintViolationException ex) {
		List<Violation> l = ex.getConstraintViolations().stream().map(e -> {
			return new Violation(e.getPropertyPath().toString(), e.getMessageTemplate());
		}).collect(Collectors.toList());
		ValidationErrorResponse v = new ValidationErrorResponse();
		v.setViolations(l);
		return v;
	}
	
	@ResponseStatus(value = HttpStatus.BAD_REQUEST)
	@ExceptionHandler(ResourceNotFoundException.class)
	@ResponseBody
	public ResponseEntity<ResponseExeption> resourceNotFoundExceptionHandler1(ResourceNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ResponseExeption(ex.getMessage()));
	}
}