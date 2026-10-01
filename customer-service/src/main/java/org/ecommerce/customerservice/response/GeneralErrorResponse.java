package org.ecommerce.customerservice.response;

import java.time.Instant;

/**
 * Standard error response envelope for customer-service APIs.
 *
 * @param errorCode HTTP-style numeric error status
 * @param errorDescription human-readable error description
 * @param time server-side timestamp when the error response was created
 */
public record GeneralErrorResponse(
        int errorCode,
        String errorDescription,
        Instant time
) {
    /**
     * Factory method that stamps the error envelope with the current time.
     */
    public static GeneralErrorResponse of(int errorCode, String errorDescription) {
        return new GeneralErrorResponse(errorCode, errorDescription, Instant.now());
    }
}

