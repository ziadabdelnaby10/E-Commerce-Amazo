package org.ecommerce.paymentservice.domain.dto.response;

import org.ecommerce.paymentservice.domain.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentTransactionResponse(
        String transactionType,
        BigDecimal amount,
        String gatewayTransactionId,
        PaymentStatus status,
        String responseCode,
        String responseMessage,
        Instant createdAt
) {
}

