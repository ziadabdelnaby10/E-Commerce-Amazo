package org.ecommerce.customerservice.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Refresh-token exchange request payload.
 *
 * @param refreshToken opaque refresh token previously issued by the service
 */
public record RefreshTokenRequest(
        @NotBlank String refreshToken
) {
}

