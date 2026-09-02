package org.ecommerce.paymentservice.exception;

public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(String paymentId) {
        super("Payment not found with paymentId: " + paymentId);
    }
}

