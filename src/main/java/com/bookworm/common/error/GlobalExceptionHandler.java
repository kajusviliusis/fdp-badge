package com.bookworm.common.error;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Translates exceptions into the error payloads defined by the OpenAPI specification.
 * Internal details are logged server-side only and never returned to the client.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(BusinessRuleException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Request body is missing or malformed.", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                            HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Invalid value for parameter '%s'.".formatted(ex.getName()), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                                    HttpServletRequest request) {
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::toFieldError)
                .toList();
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ValidationErrorResponse.of(request.getRequestURI(), fieldErrors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {}", request.getRequestURI(), ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.", request);
    }

    private static ResponseEntity<ErrorResponse> error(HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(ErrorResponse.of(status, message, request.getRequestURI()));
    }

    /** Omits the rejected value for password fields so credentials are never echoed back. */
    private static FieldError toFieldError(org.springframework.validation.FieldError error) {
        boolean sensitive = error.getField().toLowerCase().contains("password");
        String rejected = sensitive || error.getRejectedValue() == null ? null : String.valueOf(error.getRejectedValue());
        return new FieldError(error.getField(), rejected, error.getDefaultMessage());
    }
}
