package org.ecommerce.notificationservice.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import org.ecommerce.notificationservice.domain.model.NotificationPriority;
import org.ecommerce.notificationservice.domain.model.NotificationStatus;
import org.ecommerce.notificationservice.domain.model.NotificationType;

import java.time.Instant;
import java.util.List;

public record NotificationDetailResponse(
        String notificationId,
        Long userId,
        NotificationType type,
        NotificationStatus status,
        String subject,
        String body,
        String templateName,
        JsonNode templateVariables,
        String recipientAddress,
        NotificationPriority priority,
        Integer retryCount,
        Integer maxRetries,
        String failureReason,
        JsonNode metadata,
        Instant createdAt,
        Instant updatedAt,
        List<NotificationLogResponse> logs
) {
}

