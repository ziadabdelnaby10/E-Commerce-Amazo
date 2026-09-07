package org.ecommerce.notificationservice.api;

import org.ecommerce.notificationservice.api.dto.GeneralErrorResponse;
import org.ecommerce.notificationservice.application.exception.NotificationNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<GeneralErrorResponse> handleNotFound(NotificationNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(GeneralErrorResponse.of(HttpStatus.NOT_FOUND.value(), ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<GeneralErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(GeneralErrorResponse.of(HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
    }
}

