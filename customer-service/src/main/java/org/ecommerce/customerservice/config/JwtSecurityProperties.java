package org.ecommerce.customerservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Externalized JWT signing and lifetime settings.
 *
 * @param secret shared HMAC secret used for HS256 signing and validation
 * @param issuer issuer claim written into access tokens and expected during validation
 * @param accessTokenTtl lifetime of short-lived bearer access tokens
 * @param refreshTokenTtl lifetime of stored refresh tokens used for token rotation
 */
@ConfigurationProperties(prefix = "application.security.jwt")
public record JwtSecurityProperties(
        String secret,
        String issuer,
        Duration accessTokenTtl,
        Duration refreshTokenTtl
) {
}

