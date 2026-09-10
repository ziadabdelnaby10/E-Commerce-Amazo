package org.ecommerce.customerservice.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Instant expiresAt,
        Instant refreshTokenExpiresAt,
        UUID userId,
        String email,
        List<String> roles,
        List<String> permissions
) {
}

