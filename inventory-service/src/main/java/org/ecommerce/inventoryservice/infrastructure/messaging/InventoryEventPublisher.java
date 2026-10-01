package org.ecommerce.inventoryservice.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${application.kafka.topics.inventory-events:inventory-events}")
    private String inventoryEventsTopic;

    /**
     * Publishes an inventory event (InventoryReserved, InventoryReleased) to Kafka
     * @param eventType type of event (InventoryReserved, InventoryReleased)
     * @param productId product/SKU identifier
     * @param orderId order ID that triggered the reservation/release
     * @param quantity quantity reserved or released
     * @param status resulting inventory status
     */
    public void publish(String eventType, String productId, Long orderId, Integer quantity, String status) {
        InventoryEventMessage message = new InventoryEventMessage(
                UUID.randomUUID().toString(),
                eventType,
                "product-" + productId,
                LocalDateTime.now(),
                buildPayload(productId, orderId, quantity, status),
                "inventory-service"
        );

        try {
            kafkaTemplate.send(inventoryEventsTopic, productId, objectMapper.writeValueAsString(message));
            log.debug("Published inventory event {} for product {} from order {}", eventType, productId, orderId);
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize inventory event {} for product {}", eventType, productId, ex);
            throw new IllegalStateException("Failed to publish inventory event", ex);
        }
    }

    private ObjectNode buildPayload(String productId, Long orderId, Integer quantity, String status) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("productId", productId);
        payload.put("orderId", orderId);
        payload.put("quantity", quantity);
        payload.put("status", status);
        return payload;
    }
}

