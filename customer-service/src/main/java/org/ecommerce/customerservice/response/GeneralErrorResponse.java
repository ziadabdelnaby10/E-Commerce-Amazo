package org.ecommerce.customerservice.response;

import java.time.Instant;

public record GeneralErrorResponse(
        int errorCode,
        String errorDescription,
        Instant time
) {
    public static GeneralErrorResponse of(int errorCode, String errorDescription) {
        return new GeneralErrorResponse(errorCode, errorDescription, Instant.now());
    }
}

