package org.ecommerce.notificationservice.api.dto;

import java.time.Instant;

public record NotificationPreferenceResponse(
        Long userId,
        boolean emailOnOrderCreated,
        boolean emailOnOrderShipped,
        boolean emailOnOrderDelivered,
        boolean emailOnPaymentSuccess,
        boolean emailOnPaymentFailed,
        boolean emailOnInventoryAlert,
        boolean smsOnOrderShipped,
        boolean smsOnPaymentFailed,
        boolean pushOnOrderUpdate,
        boolean pushOnPaymentUpdate,
        boolean unsubscribedFromMarketing,
        boolean unsubscribedFromAll,
        Instant createdAt,
        Instant updatedAt
) {
}

