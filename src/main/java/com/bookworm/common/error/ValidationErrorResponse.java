package com.bookworm.common.error;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;

/**
 * Error payload returned when a request body fails Bean Validation.
 */
public record ValidationErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldError> fieldErrors) {

    public static ValidationErrorResponse of(String path, List<FieldError> fieldErrors) {
        HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
        return new ValidationErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(),
                "Validation failed for one or more fields.", path, fieldErrors);
    }
}
