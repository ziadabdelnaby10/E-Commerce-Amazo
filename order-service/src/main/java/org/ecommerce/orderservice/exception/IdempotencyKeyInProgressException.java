package org.ecommerce.orderservice.exception;

public class IdempotencyKeyInProgressException extends RuntimeException {

    public IdempotencyKeyInProgressException(String key) {
        super("Request with idempotency key is already being processed: " + key);
    }
}

