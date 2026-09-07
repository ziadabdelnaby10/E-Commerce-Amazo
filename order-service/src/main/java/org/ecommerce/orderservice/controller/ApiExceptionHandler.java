package org.ecommerce.orderservice.controller;

import jakarta.persistence.EntityNotFoundException;
import org.ecommerce.orderservice.domain.dto.response.GeneralErrorResponse;
import org.ecommerce.orderservice.exception.IdempotencyKeyInProgressException;
import org.ecommerce.orderservice.exception.OrderNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<GeneralErrorResponse> handleOrderNotFound(OrderNotFoundException ex) {
        return response(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<GeneralErrorResponse> handleEntityNotFound(EntityNotFoundException ex) {
        return response(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(IdempotencyKeyInProgressException.class)
    public ResponseEntity<GeneralErrorResponse> handleIdempotencyInProgress(IdempotencyKeyInProgressException ex) {
        return response(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GeneralErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        return response(HttpStatus.BAD_REQUEST, "Validation failed");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<GeneralErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return response(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    private ResponseEntity<GeneralErrorResponse> response(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(GeneralErrorResponse.of(status.value(), message));
    }
}

