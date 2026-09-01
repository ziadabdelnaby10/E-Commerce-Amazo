package org.ecommerce.orderservice.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ecommerce.orderservice.service.OrderService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private static final Pattern DIGITS_PATTERN = Pattern.compile("(\\d+)");

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${application.kafka.topics.payment-events:payment-events}",
            groupId = "${spring.kafka.consumer.group-id:order-service-payments}",
            containerFactory = "paymentEventsKafkaListenerContainerFactory"
    )
    public void consumePaymentEvents(String rawEvent) {
        PaymentEventMessage event;
        try {
            event = objectMapper.readValue(rawEvent, PaymentEventMessage.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Failed to parse payment event payload", ex);
        }

        Long orderId = extractOrderId(event);

        switch (event.eventType()) {
            case "PaymentCompleted" -> {
                orderService.markPaymentCaptured(orderId, event.eventId());
                log.info("Order {} confirmed from payment event {}", orderId, event.eventId());
            }
            case "PaymentFailed" -> {
                orderService.markPaymentFailed(orderId, event.eventId());
                log.info("Order {} cancelled from payment event {}", orderId, event.eventId());
            }
            default -> log.debug("Ignoring payment event type {}", event.eventType());
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


