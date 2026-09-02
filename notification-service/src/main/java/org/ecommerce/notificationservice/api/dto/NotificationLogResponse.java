package org.ecommerce.notificationservice.api.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

public record NotificationLogResponse(
        String action,
        JsonNode details,
        Instant timestamp
) {
}

