package org.ecommerce.notificationservice.api.dto;

import org.ecommerce.notificationservice.domain.model.NotificationPriority;
import org.ecommerce.notificationservice.domain.model.NotificationStatus;
import org.ecommerce.notificationservice.domain.model.NotificationType;

import java.time.Instant;

public record NotificationSummaryResponse(
        Long id,
        String notificationId,
        Long userId,
        NotificationType type,
        NotificationStatus status,
        String subject,
        String recipientAddress,
        NotificationPriority priority,
        Integer retryCount,
        Instant createdAt,
        Instant sentAt,
        Instant failedAt
) {
}

