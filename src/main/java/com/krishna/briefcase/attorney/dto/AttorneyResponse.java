package com.krishna.briefcase.attorney.dto;

import java.time.Instant;

public record AttorneyResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        boolean active,
        Long userId,
        Instant createdAt
) {
}
