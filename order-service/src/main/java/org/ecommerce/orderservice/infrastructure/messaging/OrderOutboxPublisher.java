package org.ecommerce.orderservice.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.ecommerce.orderservice.domain.model.OrderEvent;
import org.ecommerce.orderservice.infrastructure.persistence.repository.OrderEventJpaRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class OrderOutboxPublisher {

    private final OrderEventJpaRepository eventRepository;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OrderOutboxPublisher(
            OrderEventJpaRepository eventRepository,
            ObjectMapper objectMapper,
            @Qualifier("orderEventKafkaTemplate") KafkaTemplate<String, String> kafkaTemplate) {
        this.eventRepository = eventRepository;
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Value("${application.kafka.topics.order-events:order-events}")
    private String orderEventsTopic;

    @Value("${application.kafka.outbox.batch-size:100}")
    private int outboxBatchSize;

    @Scheduled(fixedDelayString = "${application.kafka.outbox.fixed-delay-ms:3000}")
    @Transactional
    public void publishPendingEvents() {
        List<OrderEvent> events = eventRepository.findUnpublishedForOutbox(PageRequest.of(0, outboxBatchSize));
        for (OrderEvent event : events) {
            OrderEventMessage message = new OrderEventMessage(
                    event.getEventId(),
                    event.getEventType(),
                    String.valueOf(event.getOrder().getId()),
                    "Order",
                    Instant.now(),
                    event.getOrder().getVersion() == null ? 0L : event.getOrder().getVersion(),
                    event.getEventPayload(),
                    "order-service"
            );

            try {
                String payload = objectMapper.writeValueAsString(message);
                kafkaTemplate.send(orderEventsTopic, String.valueOf(event.getOrder().getId()), payload)
                        .get(5, TimeUnit.SECONDS);
                event.setPublishedToKafka(true);
            } catch (JsonProcessingException ex) {
                log.error("Failed serializing outbox event {}", event.getEventId(), ex);
                break;
            } catch (Exception ex) {
                log.error("Failed publishing outbox event {}, will retry", event.getEventId(), ex);
                break;
            }
        }
    }
}
