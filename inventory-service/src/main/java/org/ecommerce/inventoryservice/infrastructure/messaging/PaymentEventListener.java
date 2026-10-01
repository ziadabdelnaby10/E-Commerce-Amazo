package org.ecommerce.inventoryservice.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private static final Pattern DIGITS_PATTERN = Pattern.compile("(\\d+)");

    private final ObjectMapper objectMapper;
    private final InventoryEventPublisher eventPublisher;

    /**
     * Consumes PaymentFailed events from payment-service and releases reserved inventory
     * @param rawEvent JSON-serialized PaymentEventMessage from Kafka
     */
    @KafkaListener(
            topics = "${application.kafka.topics.payment-events:payment-events}",
            groupId = "${application.kafka.consumers.payment-group-id:inventory-service-payments}",
            containerFactory = "paymentEventKafkaListenerContainerFactory"
    )
    public void consumePaymentEvents(String rawEvent) {
        PaymentEventMessage event;
        try {
            event = objectMapper.readValue(rawEvent, PaymentEventMessage.class);
        } catch (JsonProcessingException ex) {
            log.error("Failed to parse payment event payload", ex);
            throw new IllegalArgumentException("Failed to parse payment event payload", ex);
        }

        Long orderId = extractOrderId(event);

        if ("PaymentFailed".equals(event.eventType())) {
            log.info("Payment failed for order {}, releasing reserved inventory", orderId);
            try {
                // Extract product details from payload
                String productId = event.payload().hasNonNull("productId") ? 
                        event.payload().get("productId").asText() : null;
                int quantity = event.payload().hasNonNull("quantity") ? 
                        event.payload().get("quantity").asInt() : 0;

                if (productId != null && quantity > 0) {
                    eventPublisher.publish("InventoryReleased", productId, orderId, quantity, "RELEASED");
                    log.info("Inventory released for product {} in order {}", productId, orderId);
                } else {
                    log.warn("Payment failed event for order {} has no valid product/quantity information", orderId);
                }
            } catch (Exception ex) {
                log.error("Failed to release inventory on payment failure for order {}", orderId, ex);
                throw new IllegalStateException("Failed to release inventory", ex);
            }
        } else if ("PaymentCompleted".equals(event.eventType())) {
            log.debug("Payment completed for order {}, inventory remains reserved", orderId);
        }
    }

    private Long extractOrderId(PaymentEventMessage event) {
        if (event.aggregateId() != null) {
            Matcher matcher = DIGITS_PATTERN.matcher(event.aggregateId());
            if (matcher.find()) {
                return Long.parseLong(matcher.group(1));
            }
        }

        if (event.payload() != null && event.payload().hasNonNull("orderId")) {
            return event.payload().get("orderId").asLong();
        }

        throw new IllegalArgumentException("Payment event does not contain a parseable orderId");
    }
}

