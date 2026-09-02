package org.ecommerce.paymentservice.domain.dto.response;

public record InitiatePaymentResponse(
        String paymentId,
        String status,
        String reason
) {
}

