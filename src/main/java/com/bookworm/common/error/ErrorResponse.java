package com.bookworm.common.error;

import java.time.Instant;

import org.springframework.http.HttpStatus;

/**
 * Standard error payload returned for all non-validation failures.
 */
public record ErrorResponse(Instant timestamp, int status, String error, String message, String path) {

    public static ErrorResponse of(HttpStatus status, String message, String path) {
        return new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message, path);
    }
}
