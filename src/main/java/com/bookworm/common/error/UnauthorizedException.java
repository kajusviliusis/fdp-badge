package com.bookworm.common.error;

/**
 * Thrown when the caller is not authenticated or the identity presented is not valid. Mapped to HTTP 401.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
