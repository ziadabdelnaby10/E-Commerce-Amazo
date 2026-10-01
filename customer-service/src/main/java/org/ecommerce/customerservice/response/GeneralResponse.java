package org.ecommerce.customerservice.response;

import java.time.Instant;

/**
 * Standard success response envelope for customer-service APIs.
 *
 * @param statusCode HTTP-style numeric status code duplicated in the body for consistency
 * @param time server-side timestamp when the response was created
 * @param data payload returned to the client
 * @param <T> payload type
 */
public record GeneralResponse<T>(
        int statusCode,
        Instant time,
        T data
) {
    /**
     * Factory method that stamps the envelope with the current time.
     */
    public static <T> GeneralResponse<T> of(int statusCode, T data) {
        return new GeneralResponse<>(statusCode, Instant.now(), data);
    }
}

