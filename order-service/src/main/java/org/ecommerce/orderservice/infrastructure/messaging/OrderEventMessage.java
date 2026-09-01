package org.ecommerce.orderservice.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.time.LocalDateTime;

public record OrderEventMessage(
        String eventId,
        String eventType,
        String aggregateId,
        String aggregateType,
        Instant timestamp,
        long version,
        JsonNode payload,
        String source
) {
}

