package org.ecommerce.orderservice.infrastructure.client.dto;

import java.math.BigDecimal;

public record InitiatePaymentRequest(
        Long orderId,
        String userId,
        BigDecimal amount,
        String currency
) {
}

