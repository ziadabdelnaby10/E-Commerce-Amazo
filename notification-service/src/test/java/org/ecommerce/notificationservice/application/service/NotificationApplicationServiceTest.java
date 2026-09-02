package org.ecommerce.notificationservice.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ecommerce.notificationservice.application.port.out.CustomerContactPort;
import org.ecommerce.notificationservice.application.port.out.EmailSenderPort;
import org.ecommerce.notificationservice.application.port.out.NotificationEventPublisherPort;
import org.ecommerce.notificationservice.domain.model.Notification;
import org.ecommerce.notificationservice.domain.model.NotificationTemplate;
import org.ecommerce.notificationservice.domain.model.NotificationType;
import org.ecommerce.notificationservice.infrastructure.mapping.NotificationMapper;
import org.ecommerce.notificationservice.infrastructure.persistence.repository.FailedNotificationJpaRepository;
import org.ecommerce.notificationservice.infrastructure.persistence.repository.NotificationEventJpaRepository;
import org.ecommerce.notificationservice.infrastructure.persistence.repository.NotificationJpaRepository;
import org.ecommerce.notificationservice.infrastructure.persistence.repository.NotificationPreferenceJpaRepository;
import org.ecommerce.notificationservice.infrastructure.persistence.repository.NotificationTemplateJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationApplicationServiceTest {

    @Mock
    private NotificationJpaRepository notificationRepository;
    @Mock
    private NotificationEventJpaRepository eventRepository;
    @Mock
    private NotificationPreferenceJpaRepository preferenceRepository;
    @Mock
    private NotificationTemplateJpaRepository templateRepository;
    @Mock
    private FailedNotificationJpaRepository failedNotificationRepository;
    @Mock
    private NotificationMapper notificationMapper;
    @Mock
    private EmailSenderPort emailSenderPort;
    @Mock
    private CustomerContactPort customerContactPort;
    @Mock
    private NotificationEventPublisherPort eventPublisherPort;

    private NotificationApplicationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationApplicationService(
                notificationRepository,
                eventRepository,
                preferenceRepository,
                templateRepository,
                failedNotificationRepository,
                notificationMapper,
                new ObjectMapper(),
                new TemplateRenderer(),
                emailSenderPort,
                customerContactPort,
                eventPublisherPort
        );
    }

    @Test
    void processInboundEventSkipsDuplicateEventId() {
        when(eventRepository.existsByEventId("e-1")).thenReturn(true);

        service.processInboundEvent("{\"eventId\":\"e-1\",\"eventType\":\"OrderCreated\",\"aggregateId\":\"1\",\"payload\":{\"userId\":1}}");

        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void processInboundEventSendsEmailAndPublishesOutboundEvent() {
        when(eventRepository.existsByEventId("evt-2")).thenReturn(false);
        when(eventRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(customerContactPort.resolveEmailByUserId(11L)).thenReturn(Optional.of("user11@example.com"));
        when(templateRepository.findByNameAndTypeAndActiveTrue("order_confirmation", NotificationType.EMAIL)).thenReturn(Optional.of(template()));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.processInboundEvent("{" +
                "\"eventId\":\"evt-2\"," +
                "\"eventType\":\"OrderCreated\"," +
                "\"aggregateId\":\"101\"," +
                "\"aggregateType\":\"Order\"," +
                "\"payload\":{\"userId\":11,\"orderNumber\":\"ORD-101\",\"amount\":29.9}" +
                "}");

        verify(emailSenderPort, times(1)).send(any());
        verify(eventPublisherPort, times(1)).publish(any(), any(), any());
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    private NotificationTemplate template() {
        NotificationTemplate template = new NotificationTemplate();
        template.setName("order_confirmation");
        template.setType(NotificationType.EMAIL);
        template.setSubjectTemplate("Order Confirmation - {{orderNumber}}");
        template.setBodyTemplate("Amount {{amount}}");
        template.setActive(true);
        template.setLanguage("en");
        template.setCreatedAt(Instant.now());
        template.setUpdatedAt(Instant.now());
        return template;
    }
}



