package by.gdev.alert.job.core.handler;

import by.gdev.alert.job.core.exeption.ai.*;
import by.gdev.alert.job.core.exeption.ai.binding.BindingAlreadyExistsException;
import by.gdev.alert.job.core.exeption.ai.binding.BindingNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BindingAlreadyExistsException.class)
    public ResponseEntity<?> handleBindingExists(BindingAlreadyExistsException ex) {
        return ResponseEntity.status(409).body(ex.getMessage());
    }

    @ExceptionHandler(TemplateNotFoundException.class)
    public ResponseEntity<?> handleTemplateNotFound(TemplateNotFoundException ex) {
        return ResponseEntity.status(400).body(ex.getMessage());
    }

    @ExceptionHandler(BindingNotFoundException.class)
    public ResponseEntity<?> handleBindingNotFound(BindingNotFoundException ex) {
        return ResponseEntity.status(404).body(ex.getMessage());
    }

    @ExceptionHandler(UserCredentialNotFoundException.class)
    public ResponseEntity<?> handleUserCredentialNotFound(UserCredentialNotFoundException ex) {
        return ResponseEntity.status(404).body(ex.getMessage());
    }


    @ExceptionHandler(OrderModuleNotFoundException.class)
    public ResponseEntity<?> handleOrderModuleNotFound(OrderModuleNotFoundException ex) {
        return ResponseEntity.status(404).body(ex.getMessage());
    }


}

