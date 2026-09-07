package org.ecommerce.orderservice.domain.dto.response;

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

