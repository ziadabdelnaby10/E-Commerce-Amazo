package org.ecommerce.notificationservice.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ecommerce.notificationservice.application.service.NotificationApplicationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationApplicationService notificationApplicationService;

    @KafkaListener(
            topics = "${application.kafka.topics.order-events:order-events}",
            groupId = "${application.kafka.consumers.order-group-id:notification-service-orders}",
            containerFactory = "notificationKafkaListenerContainerFactory"
    )
    public void consumeOrderEvents(String rawEvent) {
        log.debug("Received order event: {}", rawEvent);
        notificationApplicationService.processInboundEvent(rawEvent);
    }

    @KafkaListener(
            topics = "${application.kafka.topics.payment-events:payment-events}",
            groupId = "${application.kafka.consumers.payment-group-id:notification-service-payments}",
            containerFactory = "notificationKafkaListenerContainerFactory"
    )
    public void consumePaymentEvents(String rawEvent) {
        log.debug("Received payment event: {}", rawEvent);
        notificationApplicationService.processInboundEvent(rawEvent);
    }
}

