package org.ecommerce.inventoryservice.model.response;

import java.time.Instant;

public record GeneralResponse<T>(
        int statusCode,
        Instant time,
        T data
) {
    public static <T> GeneralResponse<T> of(int statusCode, T data) {
        return new GeneralResponse<>(statusCode, Instant.now(), data);
    }
}

