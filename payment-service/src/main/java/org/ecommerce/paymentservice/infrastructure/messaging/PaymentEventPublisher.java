package org.ecommerce.paymentservice.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ecommerce.paymentservice.domain.model.Payment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${application.kafka.topics.payment-events:payment-events}")
    private String paymentEventsTopic;

    /**
     * @param customerEmail recipient carried from the order; embedded in the payload so
     *                      notification-service can send mail without an outbound HTTP lookup
     */
    public void publish(String eventType, Payment payment, String customerEmail) {
        PaymentEventMessage message = new PaymentEventMessage(
                UUID.randomUUID().toString(),
                eventType,
                "order-" + payment.getOrderId(),
                LocalDateTime.now(),
                buildPayload(payment, customerEmail),
                "payment-service"
        );

        try {
            kafkaTemplate.send(paymentEventsTopic, payment.getOrderId().toString(), objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize payment event {} for payment {}", eventType, payment.getPaymentId(), ex);
            throw new IllegalStateException("Failed to publish payment event", ex);
        }
    }

    /**
     * Built as an {@link ObjectNode} rather than {@code Map.of(...)} because the email may be
     * absent and {@code Map.of} rejects null values with a {@link NullPointerException}.
     */
    private ObjectNode buildPayload(Payment payment, String customerEmail) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("orderId", payment.getOrderId());
        payload.put("paymentId", payment.getPaymentId());
        payload.put("status", payment.getStatus().name());
        payload.put("amount", payment.getAmount());
        payload.put("currency", payment.getCurrency());
        payload.put("userId", payment.getUserId());
        payload.put("email", customerEmail);
        return payload;
    }
}
