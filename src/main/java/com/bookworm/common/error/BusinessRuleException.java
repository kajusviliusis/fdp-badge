package com.bookworm.common.error;

/**
 * Thrown when a request is well-formed but violates a business rule (e.g. insufficient stock). Mapped to HTTP 400.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
