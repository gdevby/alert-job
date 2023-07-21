package by.gdev.alert.job.notification.exeption;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import by.gdev.common.exeption.ResponseExeption;

@ControllerAdvice
public class ErrorHandlingController {

    @ResponseStatus(value = HttpStatus.FORBIDDEN)
    @ExceptionHandler(WebClientResponseException.Forbidden.class)
    @ResponseBody
    public ResponseExeption webExchangeBindExceptionHandler(WebClientResponseException.Forbidden ex) {
	return new ResponseExeption(ex.getMessage());
    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    @ExceptionHandler(WebClientResponseException.BadRequest.class)
    @ResponseBody
    public ResponseExeption webExchangeBindExceptionHandler(WebClientResponseException.BadRequest ex) {
	return new ResponseExeption(ex.getMessage());
    }

}
