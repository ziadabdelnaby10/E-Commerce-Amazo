package org.ecommerce.notificationservice.api.dto;

public record UpdateNotificationPreferenceRequest(
        Boolean emailOnOrderCreated,
        Boolean emailOnOrderShipped,
        Boolean emailOnOrderDelivered,
        Boolean emailOnPaymentSuccess,
        Boolean emailOnPaymentFailed,
        Boolean emailOnInventoryAlert,
        Boolean smsOnOrderShipped,
        Boolean smsOnPaymentFailed,
        Boolean pushOnOrderUpdate,
        Boolean pushOnPaymentUpdate,
        Boolean unsubscribedFromMarketing,
        Boolean unsubscribedFromAll
) {
}

