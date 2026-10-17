package com.bookworm.common.error;

/**
 * A single field-level validation failure.
 *
 * @param rejectedValue string form of the offending value; {@code null} for sensitive fields
 */
public record FieldError(String field, String rejectedValue, String message) {
}
