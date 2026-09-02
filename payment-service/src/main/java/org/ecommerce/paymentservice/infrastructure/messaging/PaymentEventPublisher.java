package org.ecommerce.paymentservice.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ecommerce.paymentservice.domain.model.Payment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${application.kafka.topics.payment-events:payment-events}")
    private String paymentEventsTopic;

    public void publish(String eventType, Payment payment) {
        PaymentEventMessage message = new PaymentEventMessage(
                UUID.randomUUID().toString(),
                eventType,
                "order-" + payment.getOrderId(),
                LocalDateTime.now(),
                objectMapper.valueToTree(Map.of(
                        "orderId", payment.getOrderId(),
                        "paymentId", payment.getPaymentId(),
                        "status", payment.getStatus().name(),
                        "amount", payment.getAmount(),
                        "currency", payment.getCurrency(),
                        "userId", payment.getUserId()
                )),
                "payment-service"
        );

        try {
            kafkaTemplate.send(paymentEventsTopic, payment.getOrderId().toString(), objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize payment event {} for payment {}", eventType, payment.getPaymentId(), ex);
            throw new IllegalStateException("Failed to publish payment event", ex);
        }
    }
}

