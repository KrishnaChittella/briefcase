package com.krishna.briefcase.common.exception;

/**
 * Thrown when a requested entity does not exist. Mapped to HTTP 404 by the
 * global exception handler (Phase 2).
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException of(String entity, Object id) {
        return new ResourceNotFoundException(entity + " not found with id " + id);
    }
}
