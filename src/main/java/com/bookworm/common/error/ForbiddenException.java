package com.bookworm.common.error;

/**
 * Thrown when an authenticated user tries to access a resource owned by someone else. Mapped to HTTP 403.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
