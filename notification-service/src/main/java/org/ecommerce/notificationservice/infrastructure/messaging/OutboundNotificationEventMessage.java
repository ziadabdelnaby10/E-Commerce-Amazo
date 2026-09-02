package org.ecommerce.notificationservice.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

public record OutboundNotificationEventMessage(
        String eventId,
        String eventType,
        String aggregateId,
        String aggregateType,
        Instant timestamp,
        JsonNode payload,
        String source
) {
}

