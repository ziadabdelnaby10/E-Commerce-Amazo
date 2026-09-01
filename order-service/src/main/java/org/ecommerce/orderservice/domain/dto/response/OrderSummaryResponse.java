package org.ecommerce.orderservice.domain.dto.response;

import org.ecommerce.orderservice.domain.model.OrderStatus;
import org.ecommerce.orderservice.domain.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

public record OrderSummaryResponse(
        Long id,
        String orderNumber,
        OrderStatus status,
        PaymentStatus paymentStatus,
        BigDecimal totalAmount,
        String currency,
        Instant createdAt
) {
}

