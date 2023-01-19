package by.gdev.alert.job.core.exeption;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.support.WebExchangeBindException;

import by.gdev.common.exeption.ResourceNotFoundException;

@ControllerAdvice
public class ErrorHandlingController {
	
	@ResponseStatus(value = HttpStatus.BAD_REQUEST)
	@ExceptionHandler(WebExchangeBindException.class)
	@ResponseBody
	public List<ResponseExeption> webExchangeBindExceptionHandler(WebExchangeBindException ex) {
		return ex.getBindingResult().getAllErrors().stream().map(e -> new ResponseExeption(e.getDefaultMessage())).toList();
	}
	
	@ResponseStatus(value = HttpStatus.NOT_FOUND)
	@ExceptionHandler(ResourceNotFoundException.class)
	@ResponseBody
	public ResponseExeption resourceNotFoundExceptionHandler(ResourceNotFoundException ex) {
		return new ResponseExeption(ex.getMessage());
	}
}