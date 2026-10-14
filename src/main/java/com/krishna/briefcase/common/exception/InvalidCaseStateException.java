package com.krishna.briefcase.common.exception;

/**
 * Thrown when an operation is not valid for a case's current state
 * (e.g. an illegal status transition, or resolving an already-resolved case).
 * Mapped to HTTP 409.
 */
public class InvalidCaseStateException extends RuntimeException {

    public InvalidCaseStateException(String message) {
        super(message);
    }
}
