package org.ecommerce.inventoryservice.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

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

