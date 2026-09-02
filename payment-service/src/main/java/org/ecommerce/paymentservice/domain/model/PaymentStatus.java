package org.ecommerce.paymentservice.domain.model;

public enum PaymentStatus {
    PENDING,
    AUTHORIZED,
    CAPTURED,
    DECLINED,
    FAILED,
    REFUNDED,
    CANCELLED
}

