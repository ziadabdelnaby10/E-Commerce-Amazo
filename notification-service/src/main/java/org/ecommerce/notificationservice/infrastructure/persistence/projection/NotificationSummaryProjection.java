package org.ecommerce.notificationservice.infrastructure.persistence.projection;

import org.ecommerce.notificationservice.domain.model.NotificationPriority;
import org.ecommerce.notificationservice.domain.model.NotificationStatus;
import org.ecommerce.notificationservice.domain.model.NotificationType;

import java.time.Instant;

public interface NotificationSummaryProjection {
    Long getId();

    String getNotificationId();

    Long getUserId();

    NotificationType getType();

    NotificationStatus getStatus();

    String getSubject();

    String getRecipientAddress();

    NotificationPriority getPriority();

    Integer getRetryCount();

    Instant getCreatedAt();

    Instant getSentAt();

    Instant getFailedAt();
}

