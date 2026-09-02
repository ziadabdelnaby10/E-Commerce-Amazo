package org.ecommerce.notificationservice.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.ecommerce.notificationservice.application.port.out.NotificationEventPublisherPort;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@Slf4j
public class KafkaNotificationEventPublisher implements NotificationEventPublisherPort {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaNotificationEventPublisher(
            @Qualifier("notificationKafkaTemplate") KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Value("${application.kafka.topics.notification-events:notification-events}")
    private String topic;

    @Override
    public void publish(String eventType, String aggregateId, JsonNode payload) {
        OutboundNotificationEventMessage message = new OutboundNotificationEventMessage(
                UUID.randomUUID().toString(),
                eventType,
                aggregateId,
                "Notification",
                Instant.now(),
                payload,
                "notification-service"
        );

        try {
            kafkaTemplate.send(topic, aggregateId, objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize outbound notification event {}", eventType, ex);
        }
    }
}


