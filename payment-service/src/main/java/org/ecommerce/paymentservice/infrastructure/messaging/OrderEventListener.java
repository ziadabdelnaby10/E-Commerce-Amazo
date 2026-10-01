package org.ecommerce.paymentservice.infrastructure.messaging;

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
public class OrderEventListener {

    private static final Pattern DIGITS_PATTERN = Pattern.compile("(\\d+)");

    private final ObjectMapper objectMapper;

    /**
     * Consumes OrderCreated events from order-service
     * This is a reactive trigger point for payment processing in event-driven architecture
     * @param rawEvent JSON-serialized OrderEventMessage from Kafka
     */
    @KafkaListener(
            topics = "${application.kafka.topics.order-events:order-events}",
            groupId = "${spring.kafka.consumer.group-id:payment-service-orders}",
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
            log.info("Received OrderCreated event for order {}, payment processing will be initiated", orderId);
            try {
                // Extract payment details from order payload
                if (event.payload() != null) {
                    long amount = event.payload().hasNonNull("totalAmount") ? 
                            event.payload().get("totalAmount").asLong() : 0;
                    String currency = event.payload().hasNonNull("currency") ? 
                            event.payload().get("currency").asText() : "USD";
                    String userId = event.payload().hasNonNull("userId") ? 
                            event.payload().get("userId").asText() : null;

                    log.info("Order {} has amount {} {}, user: {}", orderId, amount, currency, userId);
                    // Payment processing will be initiated by the Order Service via REST call
                    // This listener serves as an audit point in the event stream
                }
            } catch (Exception ex) {
                log.error("Failed to process OrderCreated event for order {}", orderId, ex);
                throw new IllegalStateException("Failed to process order event", ex);
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

