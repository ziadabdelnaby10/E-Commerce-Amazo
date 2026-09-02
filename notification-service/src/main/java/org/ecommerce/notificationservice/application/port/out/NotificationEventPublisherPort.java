package org.ecommerce.notificationservice.application.port.out;

import com.fasterxml.jackson.databind.JsonNode;

public interface NotificationEventPublisherPort {
    void publish(String eventType, String aggregateId, JsonNode payload);
}

