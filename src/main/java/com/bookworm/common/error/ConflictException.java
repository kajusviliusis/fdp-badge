package com.bookworm.common.error;

/**
 * Thrown when a request conflicts with the current resource state (e.g. duplicate email). Mapped to HTTP 409.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
