package org.ecommerce.orderservice.infrastructure.persistence.projection;

import org.ecommerce.orderservice.domain.model.OrderStatus;
import org.ecommerce.orderservice.domain.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

public interface OrderSummaryProjection {

    Long getId();

    String getOrderNumber();

    OrderStatus getStatus();

    PaymentStatus getPaymentStatus();

    BigDecimal getTotalAmount();

    String getCurrency();

    Instant getCreatedAt();
}

