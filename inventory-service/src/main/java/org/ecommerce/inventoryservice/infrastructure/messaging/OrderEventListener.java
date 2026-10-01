package org.ecommerce.inventoryservice.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ecommerce.inventoryservice.service.StockService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private static final Pattern DIGITS_PATTERN = Pattern.compile("(\\d+)");

    private final StockService stockService;
    private final ObjectMapper objectMapper;
    private final InventoryEventPublisher eventPublisher;

    /**
     * Consumes OrderCreated events from order-service and reserves inventory
     * @param rawEvent JSON-serialized OrderEventMessage from Kafka
     */
    @KafkaListener(
            topics = "${application.kafka.topics.order-events:order-events}",
            groupId = "${application.kafka.consumers.order-group-id:inventory-service-orders}",
            containerFactory = "orderEventKafkaListenerContainerFactory"
    )
    public void consumeOrderEvents(String rawEvent) {
        OrderEventMessage event;
        try {
            event = objectMapper.readValue(rawEvent, OrderEventMessage.class);
        } catch (JsonProcessingException ex) {
            log.error("Failed to parse order event payload", ex);
            throw new IllegalArgumentException("Failed to parse order event payload", ex);
        }

        Long orderId = extractOrderId(event);

        if ("OrderCreated".equals(event.eventType())) {
            log.info("Attempting to reserve inventory for order {}", orderId);
            try {
                // The payload should contain order items with product IDs and quantities
                String productId = event.payload().hasNonNull("productId") ? 
                        event.payload().get("productId").asText() : null;
                int quantity = event.payload().hasNonNull("quantity") ? 
                        event.payload().get("quantity").asInt() : 0;

                if (productId != null && quantity > 0) {
                    eventPublisher.publish("InventoryReserved", productId, orderId, quantity, "RESERVED");
                    log.info("Inventory reserved for product {} in order {}", productId, orderId);
                } else {
                    log.warn("Order {} has no valid product/quantity information", orderId);
                }
            } catch (Exception ex) {
                log.error("Failed to process OrderCreated event for order {}", orderId, ex);
                throw new IllegalStateException("Failed to reserve inventory", ex);
            }
        }
    }

    private Long extractOrderId(OrderEventMessage event) {
        if (event.aggregateId() != null) {
            Matcher matcher = DIGITS_PATTERN.matcher(event.aggregateId());
            if (matcher.find()) {
                return Long.parseLong(matcher.group(1));
            }
        }

        if (event.payload() != null && event.payload().hasNonNull("orderId")) {
            return event.payload().get("orderId").asLong();
        }

        throw new IllegalArgumentException("Order event does not contain a parseable orderId");
    }
}

