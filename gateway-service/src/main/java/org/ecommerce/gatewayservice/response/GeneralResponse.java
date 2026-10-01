package org.ecommerce.gatewayservice.response;

import java.time.Instant;

/**
 * Generic API response wrapper used by gateway-generated responses such as fallback payloads.
 *
 * @param statusCode HTTP-style numeric status code carried in the body for consistency
 * @param time server-side timestamp when the response object was created
 * @param data payload returned to the caller
 * @param <T> type of the wrapped payload
 */
public record GeneralResponse<T>(
        int statusCode,
        Instant time,
        T data
) {
    /**
     * Factory method that stamps the response with the current time.
     *
     * @param statusCode HTTP-style status code
     * @param data payload to return
     * @return populated response wrapper
     * @param <T> type of the payload
     */
    public static <T> GeneralResponse<T> of(int statusCode, T data) {
        return new GeneralResponse<>(statusCode, Instant.now(), data);
    }
}

