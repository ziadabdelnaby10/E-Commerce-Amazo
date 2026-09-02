package org.ecommerce.paymentservice.domain.dto.response;

import org.ecommerce.paymentservice.domain.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentSummaryResponse(
        String paymentId,
        Long orderId,
        PaymentStatus status,
        BigDecimal amount,
        String currency,
        Instant createdAt
) {
}

