package com.krishna.briefcase.attorney.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AttorneyRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Email @Size(max = 255) String email,
        Boolean active,
        /** Optional: links this attorney record to a login User (an attorney logs in as themselves). */
        Long userId
) {
}
