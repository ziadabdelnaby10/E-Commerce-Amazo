package org.ecommerce.notificationservice.application.usecase.model;

import com.fasterxml.jackson.databind.JsonNode;

public record InboundEvent(
        String eventId,
        String eventType,
        String aggregateId,
        String aggregateType,
        JsonNode payload,
        String source
) {
}

