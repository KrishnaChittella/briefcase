package com.krishna.briefcase.auth.dto;

import com.krishna.briefcase.auth.UserRole;

public record AuthResponse(
        String token,
        String tokenType,
        String username,
        UserRole role,
        long expiresInMs
) {
    public static AuthResponse bearer(String token, String username, UserRole role, long expiresInMs) {
        return new AuthResponse(token, "Bearer", username, role, expiresInMs);
    }
}
