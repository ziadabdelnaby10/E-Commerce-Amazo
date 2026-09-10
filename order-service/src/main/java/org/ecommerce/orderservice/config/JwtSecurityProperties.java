package org.ecommerce.orderservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "application.security.jwt")
public record JwtSecurityProperties(
        String secret,
        String issuer,
        Duration accessTokenTtl
) {}

