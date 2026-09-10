package org.ecommerce.gatewayservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "application.security.jwt")
public record JwtSecurityProperties(
        String secret,
        String issuer
) {
}

