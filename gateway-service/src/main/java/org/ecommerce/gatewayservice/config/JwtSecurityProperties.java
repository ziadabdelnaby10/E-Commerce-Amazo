package org.ecommerce.gatewayservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized JWT verification settings for the gateway.
 *
 * @param secret shared HMAC secret used to validate HS256 signatures
 * @param issuer expected token issuer; may be blank when issuer validation is disabled
 */
@ConfigurationProperties(prefix = "application.security.jwt")
public record JwtSecurityProperties(
        String secret,
        String issuer
) {
}

