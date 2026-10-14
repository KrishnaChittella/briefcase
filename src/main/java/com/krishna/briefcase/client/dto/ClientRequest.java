package com.krishna.briefcase.client.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Create/update payload for a client.
 */
public record ClientRequest(
        @NotBlank @Size(max = 200) String name,
        @Email @Size(max = 255) String email,
        @Size(max = 50) String phone,
        @Size(max = 500) String address
) {
}
