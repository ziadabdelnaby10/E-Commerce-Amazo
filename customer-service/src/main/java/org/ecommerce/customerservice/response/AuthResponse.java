package org.ecommerce.customerservice.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Authentication response returned after login or refresh.
 *
 * @param accessToken signed bearer token used for authenticated requests
 * @param refreshToken opaque token used to obtain a new access token
 * @param tokenType token scheme, typically {@code Bearer}
 * @param expiresAt access token expiration instant
 * @param refreshTokenExpiresAt refresh token expiration instant
 * @param userId authenticated customer identifier
 * @param email authenticated customer email
 * @param roles logical role names granted to the user
 * @param permissions fine-grained permissions derived from those roles
 */
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

