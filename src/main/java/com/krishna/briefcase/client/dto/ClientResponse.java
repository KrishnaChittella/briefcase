package com.krishna.briefcase.client.dto;

import java.time.Instant;

public record ClientResponse(
        Long id,
        String name,
        String email,
        String phone,
        String address,
        Instant createdAt
) {
}
