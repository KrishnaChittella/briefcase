package com.krishna.briefcase.common.exception;

/**
 * Thrown when creating an entity would violate a uniqueness rule
 * (e.g. duplicate case number or attorney email). Mapped to HTTP 409.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
