package org.ecommerce.notificationservice.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;

public record InboundEventMessage(
        String eventId,
        String eventType,
        String aggregateId,
        String aggregateType,
        JsonNode payload,
        String source
) {
}

