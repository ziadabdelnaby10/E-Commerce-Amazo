package org.ecommerce.paymentservice.infrastructure.persistence.projection;

import org.ecommerce.paymentservice.domain.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public interface PaymentSummaryProjection {
    String getPaymentId();

    Long getOrderId();

    PaymentStatus getStatus();

    BigDecimal getAmount();

    String getCurrency();

    Instant getCreatedAt();
}

