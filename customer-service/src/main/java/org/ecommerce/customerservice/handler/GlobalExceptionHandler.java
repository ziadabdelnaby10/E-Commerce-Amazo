package org.ecommerce.customerservice.handler;

import org.ecommerce.customerservice.exception.*;
import org.ecommerce.customerservice.response.GeneralErrorResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * Centralized translation of service exceptions into API error responses.
 *
 * <p>The handler keeps controller code free from repetitive HTTP mapping logic and
 * ensures validation, authentication, authorization, lookup, and duplication errors
 * all produce the same response envelope shape.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Maps missing-customer failures to HTTP 404. */
    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<GeneralErrorResponse> handleCustomerNotFoundException(CustomerNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(GeneralErrorResponse.of(HttpStatus.NOT_FOUND.value(), ex.getMessage()));
    }

    /** Maps brute-force lockout failures to HTTP 429. */
    @ExceptionHandler(MaxAttemptsException.class)
    public ResponseEntity<GeneralErrorResponse> handleMaxAttemptsException(MaxAttemptsException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(GeneralErrorResponse.of(HttpStatus.TOO_MANY_REQUESTS.value(), ex.getMessage()));
    }

    /** Maps login failures to HTTP 401. */
    @ExceptionHandler(InvalidUsernameOrPassword.class)
    public ResponseEntity<GeneralErrorResponse> handleInvalidUsernameOrPassword(InvalidUsernameOrPassword ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(GeneralErrorResponse.of(HttpStatus.UNAUTHORIZED.value(), ex.getMessage()));
    }

    /** Maps refresh-token failures to HTTP 401. */
    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<GeneralErrorResponse> handleInvalidRefreshTokenException(InvalidRefreshTokenException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(GeneralErrorResponse.of(HttpStatus.UNAUTHORIZED.value(), ex.getMessage()));
    }

    /** Maps duplicate email registration/update failures to HTTP 409. */
    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<GeneralErrorResponse> handleDuplicateEmailException(DuplicateEmailException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(GeneralErrorResponse.of(HttpStatus.CONFLICT.value(), ex.getMessage()));
    }

    /** Maps RBAC creation/configuration conflicts to HTTP 409. */
    @ExceptionHandler({DuplicateRoleException.class, DuplicatePermissionException.class, DefaultRoleMissingException.class})
    public ResponseEntity<GeneralErrorResponse> handleConflictExceptions(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(GeneralErrorResponse.of(HttpStatus.CONFLICT.value(), ex.getMessage()));
    }

    /** Maps missing RBAC records to HTTP 404. */
    @ExceptionHandler({RoleNotFoundException.class, PermissionNotFoundException.class})
    public ResponseEntity<GeneralErrorResponse> handleRbacNotFoundExceptions(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(GeneralErrorResponse.of(HttpStatus.NOT_FOUND.value(), ex.getMessage()));
    }

    /** Maps authorization failures to HTTP 403. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<GeneralErrorResponse> handleAccessDeniedException(AccessDeniedException ignored) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(GeneralErrorResponse.of(HttpStatus.FORBIDDEN.value(), "Access denied"));
    }

    /** Maps path/query type mismatches to HTTP 400 with a friendly message. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<GeneralErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        if ("customerId".equals(ex.getName())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(GeneralErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "Invalid customer id format"));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(GeneralErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "Invalid request parameter"));
    }

    /** Collapses bean-validation errors into a single readable HTTP 400 response. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GeneralErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + messageOrDefault(fieldError))
                .collect(Collectors.joining(", "));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(GeneralErrorResponse.of(HttpStatus.BAD_REQUEST.value(), errors));
    }

    /** Returns a field error message, falling back to a generic validation message when needed. */
    private String messageOrDefault(FieldError fieldError) {
        return fieldError.getDefaultMessage() == null ? "Validation failed" : fieldError.getDefaultMessage();
    }
}
