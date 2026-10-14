package com.krishna.briefcase.common;

import java.time.Instant;
import java.util.List;

/**
 * Consistent error envelope returned by {@link GlobalExceptionHandler} across the whole API.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldError> fieldErrors
) {
    public record FieldError(String field, String message) {
    }

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now(), status, error, message, path, List.of());
    }
}
