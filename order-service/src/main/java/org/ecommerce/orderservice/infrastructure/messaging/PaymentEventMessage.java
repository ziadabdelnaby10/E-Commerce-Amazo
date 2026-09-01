package org.ecommerce.orderservice.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;

public record PaymentEventMessage(
        String eventId,
        String eventType,
        String aggregateId,
        LocalDateTime timestamp,
        JsonNode payload,
        String source
) {
}

