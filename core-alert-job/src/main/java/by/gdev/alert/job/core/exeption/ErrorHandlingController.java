package by.gdev.alert.job.core.exeption;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.support.WebExchangeBindException;

import by.gdev.common.exeption.CollectionLimitExeption;
import by.gdev.common.exeption.ConflictExeption;
import by.gdev.common.exeption.ResourceNotFoundException;
import by.gdev.common.exeption.ResponseExeption;

@ControllerAdvice
public class ErrorHandlingController {

    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    @ExceptionHandler(WebExchangeBindException.class)
    @ResponseBody
    public List<ResponseExeption> webExchangeBindExceptionHandler(WebExchangeBindException ex) {
	return ex.getBindingResult().getAllErrors().stream().map(e -> new ResponseExeption(e.getDefaultMessage()))
		.toList();
    }

    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseBody
    public ResponseExeption resourceNotFoundExceptionHandler(ResourceNotFoundException ex) {
	return new ResponseExeption(ex.getMessage());
    }

    @ResponseStatus(value = HttpStatus.CONFLICT)
    @ExceptionHandler(ConflictExeption.class)
    @ResponseBody
    public ResponseExeption conflictExceptionHandler(ConflictExeption ex) {
	return new ResponseExeption(ex.getMessage());
    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    @ExceptionHandler(CollectionLimitExeption.class)
    @ResponseBody
    public ResponseExeption conflictExceptionHandler(CollectionLimitExeption ex) {
	return new ResponseExeption(ex.getMessage());
    }
}