package org.ecommerce.orderservice.infrastructure.client.dto;

public record InitiatePaymentResponse(
        String paymentId,
        String status,
        String reason
) {

    public boolean accepted() {
        return status != null && (status.equalsIgnoreCase("PENDING") || status.equalsIgnoreCase("AUTHORIZED"));
    }

    public static InitiatePaymentResponse failed(String reason) {
        return new InitiatePaymentResponse(null, "FAILED", reason);
    }
}

