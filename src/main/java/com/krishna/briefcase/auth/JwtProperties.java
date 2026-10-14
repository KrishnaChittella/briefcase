package com.krishna.briefcase.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds the {@code jwt.*} settings from application.yml.
 *
 * @param secret     Base64-encoded HMAC signing key (>= 256 bits).
 * @param expiration Access-token lifetime in milliseconds.
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(String secret, long expiration) {
}
