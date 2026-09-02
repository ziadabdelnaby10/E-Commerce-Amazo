package org.ecommerce.paymentservice.domain.dto.response;

import org.ecommerce.paymentservice.domain.model.PaymentMethodType;
import org.ecommerce.paymentservice.domain.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PaymentResponse(
        String paymentId,
        Long orderId,
        String userId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        PaymentMethodType paymentMethod,
        Instant createdAt,
        Instant updatedAt,
        Instant authorizedAt,
        Instant failedAt,
        List<PaymentTransactionResponse> transactions
) {
}

