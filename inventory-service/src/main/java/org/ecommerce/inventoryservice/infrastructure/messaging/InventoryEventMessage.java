package org.ecommerce.inventoryservice.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;

public record InventoryEventMessage(
        String eventId,
        String eventType,
        String aggregateId,
        LocalDateTime timestamp,
        JsonNode payload,
        String source
) {
}

