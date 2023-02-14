package by.gdev.alert.job.parser.exeption;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import by.gdev.common.exeption.ResourceNotFoundException;
import by.gdev.common.exeption.ResponseExeption;

@ControllerAdvice
public class ErrorHandlingController {

	@ResponseStatus(value = HttpStatus.NOT_FOUND)
	@ExceptionHandler(ResourceNotFoundException.class)
	@ResponseBody
	public ResponseExeption resourceNotFoundExceptionHandler(ResourceNotFoundException ex) {
		return new ResponseExeption(ex.getMessage());
	}
}
