package com.bookworm.common.error;

/**
 * Thrown when a requested resource does not exist (or is not visible to the caller). Mapped to HTTP 404.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException of(String resource, Object id) {
        return new ResourceNotFoundException("%s with id %s was not found.".formatted(resource, id));
    }
}
