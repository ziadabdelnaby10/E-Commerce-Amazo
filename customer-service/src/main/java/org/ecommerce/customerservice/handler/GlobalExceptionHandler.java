package org.ecommerce.customerservice.handler;

import org.ecommerce.customerservice.exception.CustomerNotFoundException;
import org.ecommerce.customerservice.exception.DefaultRoleMissingException;
import org.ecommerce.customerservice.exception.DuplicatePermissionException;
import org.ecommerce.customerservice.exception.DuplicateEmailException;
import org.ecommerce.customerservice.exception.DuplicateRoleException;
import org.ecommerce.customerservice.exception.PermissionNotFoundException;
import org.ecommerce.customerservice.exception.RoleNotFoundException;
import org.ecommerce.customerservice.response.GeneralErrorResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<GeneralErrorResponse> handleCustomerNotFoundException(CustomerNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(GeneralErrorResponse.of(HttpStatus.NOT_FOUND.value(), ex.getMessage()));
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<GeneralErrorResponse> handleDuplicateEmailException(DuplicateEmailException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(GeneralErrorResponse.of(HttpStatus.CONFLICT.value(), ex.getMessage()));
    }

    @ExceptionHandler({DuplicateRoleException.class, DuplicatePermissionException.class, DefaultRoleMissingException.class})
    public ResponseEntity<GeneralErrorResponse> handleConflictExceptions(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(GeneralErrorResponse.of(HttpStatus.CONFLICT.value(), ex.getMessage()));
    }

    @ExceptionHandler({RoleNotFoundException.class, PermissionNotFoundException.class})
    public ResponseEntity<GeneralErrorResponse> handleRbacNotFoundExceptions(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(GeneralErrorResponse.of(HttpStatus.NOT_FOUND.value(), ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<GeneralErrorResponse> handleAccessDeniedException(AccessDeniedException ignored) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(GeneralErrorResponse.of(HttpStatus.FORBIDDEN.value(), "Access denied"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<GeneralErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        if ("customerId".equals(ex.getName())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(GeneralErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "Invalid customer id format"));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(GeneralErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "Invalid request parameter"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GeneralErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + messageOrDefault(fieldError))
                .collect(Collectors.joining(", "));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(GeneralErrorResponse.of(HttpStatus.BAD_REQUEST.value(), errors));
    }

    private String messageOrDefault(FieldError fieldError) {
        return fieldError.getDefaultMessage() == null ? "Validation failed" : fieldError.getDefaultMessage();
    }
}
