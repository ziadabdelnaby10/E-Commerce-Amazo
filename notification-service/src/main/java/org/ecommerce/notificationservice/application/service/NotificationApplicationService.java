package org.ecommerce.notificationservice.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ecommerce.notificationservice.api.dto.NotificationDetailResponse;
import org.ecommerce.notificationservice.api.dto.NotificationPreferenceResponse;
import org.ecommerce.notificationservice.api.dto.NotificationSummaryResponse;
import org.ecommerce.notificationservice.api.dto.UpdateNotificationPreferenceRequest;
import org.ecommerce.notificationservice.application.exception.NotificationNotFoundException;
import org.ecommerce.notificationservice.application.port.out.CustomerContactPort;
import org.ecommerce.notificationservice.application.port.out.EmailSenderPort;
import org.ecommerce.notificationservice.application.port.out.NotificationEventPublisherPort;
import org.ecommerce.notificationservice.application.usecase.model.InboundEvent;
import org.ecommerce.notificationservice.application.usecase.model.SendEmailCommand;
import org.ecommerce.notificationservice.domain.model.FailedNotification;
import org.ecommerce.notificationservice.domain.model.Notification;
import org.ecommerce.notificationservice.domain.model.NotificationEvent;
import org.ecommerce.notificationservice.domain.model.NotificationPreference;
import org.ecommerce.notificationservice.domain.model.NotificationPriority;
import org.ecommerce.notificationservice.domain.model.NotificationStatus;
import org.ecommerce.notificationservice.domain.model.NotificationTemplate;
import org.ecommerce.notificationservice.domain.model.NotificationType;
import org.ecommerce.notificationservice.infrastructure.mapping.NotificationMapper;
import org.ecommerce.notificationservice.infrastructure.messaging.InboundEventMessage;
import org.ecommerce.notificationservice.infrastructure.persistence.repository.FailedNotificationJpaRepository;
import org.ecommerce.notificationservice.infrastructure.persistence.repository.NotificationEventJpaRepository;
import org.ecommerce.notificationservice.infrastructure.persistence.repository.NotificationJpaRepository;
import org.ecommerce.notificationservice.infrastructure.persistence.repository.NotificationPreferenceJpaRepository;
import org.ecommerce.notificationservice.infrastructure.persistence.repository.NotificationTemplateJpaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationApplicationService {

    private static final Pattern DIGITS_PATTERN = Pattern.compile("(\\\\d+)");

    private final NotificationJpaRepository notificationRepository;
    private final NotificationEventJpaRepository eventRepository;
    private final NotificationPreferenceJpaRepository preferenceRepository;
    private final NotificationTemplateJpaRepository templateRepository;
    private final FailedNotificationJpaRepository failedNotificationRepository;
    private final NotificationMapper notificationMapper;
    private final ObjectMapper objectMapper;
    private final TemplateRenderer templateRenderer;
    private final EmailSenderPort emailSenderPort;
    private final CustomerContactPort customerContactPort;
    private final NotificationEventPublisherPort eventPublisherPort;

    @Value("${application.retry.batch-size:100}")
    private int retryBatchSize;

    @Value("${application.retry.base-delay-minutes:5}")
    private long baseRetryDelayMinutes;

    @Transactional(readOnly = true)
    public Page<NotificationSummaryResponse> getInbox(Long userId, NotificationType type, NotificationStatus status, Pageable pageable) {
        if (type != null) {
            return notificationRepository.findSummariesByUserAndType(userId, type, pageable).map(notificationMapper::toSummary);
        }
        if (status != null) {
            return notificationRepository.findSummariesByUserAndStatus(userId, status, pageable).map(notificationMapper::toSummary);
        }
        return notificationRepository.findSummariesByUser(userId, pageable).map(notificationMapper::toSummary);
    }

    @Transactional(readOnly = true)
    public NotificationDetailResponse getByNotificationId(String notificationId) {
        Notification notification = notificationRepository.findByNotificationIdWithLogs(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));
        return notificationMapper.toDetail(notification);
    }

    @Transactional(readOnly = true)
    public NotificationPreferenceResponse getPreferences(Long userId) {
        NotificationPreference preference = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> buildDefaultPreference(userId));
        return notificationMapper.toPreferenceResponse(preference);
    }

    @Transactional
    public NotificationPreferenceResponse updatePreferences(Long userId, UpdateNotificationPreferenceRequest request) {
        NotificationPreference preference = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> buildDefaultPreference(userId));

        if (request.emailOnOrderCreated() != null) preference.setEmailOnOrderCreated(request.emailOnOrderCreated());
        if (request.emailOnOrderShipped() != null) preference.setEmailOnOrderShipped(request.emailOnOrderShipped());
        if (request.emailOnOrderDelivered() != null) preference.setEmailOnOrderDelivered(request.emailOnOrderDelivered());
        if (request.emailOnPaymentSuccess() != null) preference.setEmailOnPaymentSuccess(request.emailOnPaymentSuccess());
        if (request.emailOnPaymentFailed() != null) preference.setEmailOnPaymentFailed(request.emailOnPaymentFailed());
        if (request.emailOnInventoryAlert() != null) preference.setEmailOnInventoryAlert(request.emailOnInventoryAlert());
        if (request.smsOnOrderShipped() != null) preference.setSmsOnOrderShipped(request.smsOnOrderShipped());
        if (request.smsOnPaymentFailed() != null) preference.setSmsOnPaymentFailed(request.smsOnPaymentFailed());
        if (request.pushOnOrderUpdate() != null) preference.setPushOnOrderUpdate(request.pushOnOrderUpdate());
        if (request.pushOnPaymentUpdate() != null) preference.setPushOnPaymentUpdate(request.pushOnPaymentUpdate());
        if (request.unsubscribedFromMarketing() != null) preference.setUnsubscribedFromMarketing(request.unsubscribedFromMarketing());
        if (request.unsubscribedFromAll() != null) preference.setUnsubscribedFromAll(request.unsubscribedFromAll());

        preference.setUpdatedAt(Instant.now());
        NotificationPreference saved = preferenceRepository.save(preference);
        return notificationMapper.toPreferenceResponse(saved);
    }

    @Transactional
    public void processInboundEvent(String rawEvent) {
        InboundEvent event = parseInboundEvent(rawEvent);
        if (event.eventId() == null || event.eventId().isBlank()) {
            log.warn("Skipping event without eventId");
            return;
        }
        if (eventRepository.existsByEventId(event.eventId())) {
            log.debug("Skipping duplicate notification event {}", event.eventId());
            return;
        }

        NotificationEvent persistedEvent = persistInboundEvent(event);

        try {
            Notification notification = createNotificationForEvent(event);
            if (notification == null) {
                markEventProcessed(persistedEvent, null);
                return;
            }

            trySendNotification(notification, event);
            markEventProcessed(persistedEvent, null);
        } catch (Exception ex) {
            log.error("Notification processing failed for event {}", event.eventId(), ex);
            markEventProcessed(persistedEvent, ex.getMessage());
        }
    }

    @Scheduled(fixedDelayString = "${application.retry.fixed-delay-ms:30000}")
    @Transactional
    public void retryFailedNotifications() {
        List<FailedNotification> ready = failedNotificationRepository.findReadyForRetry(
                Instant.now(),
                PageRequest.of(0, retryBatchSize)
        );

        for (FailedNotification failed : ready) {
            Notification notification = failed.getNotification();
            try {
                emailSenderPort.send(new SendEmailCommand(
                        notification.getRecipientAddress(),
                        notification.getSubject(),
                        notification.getBody(),
                        true
                ));
                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(Instant.now());
                notification.setFailureReason(null);
                notification.setNextRetryAt(null);
                notification.setUpdatedAt(Instant.now());
                appendLog(notification, "RETRIED", Map.of("result", "sent"));
                failedNotificationRepository.delete(failed);
                eventPublisherPort.publish("EmailNotificationSent", notification.getNotificationId(), buildNotificationPayload(notification));
            } catch (Exception ex) {
                int nextAttempt = failed.getRetryCount() + 1;
                notification.setRetryCount(nextAttempt);
                notification.setUpdatedAt(Instant.now());
                notification.setFailureReason(ex.getMessage());

                if (nextAttempt >= notification.getMaxRetries()) {
                    failed.setNextRetryTime(null);
                } else {
                    Instant nextRetry = Instant.now().plus(baseRetryDelayMinutes * nextAttempt, ChronoUnit.MINUTES);
                    failed.setNextRetryTime(nextRetry);
                    notification.setNextRetryAt(nextRetry);
                }
                failed.setRetryCount(nextAttempt);
                failed.setReason(ex.getMessage());
                appendLog(notification, "RETRIED", Map.of("result", "failed", "reason", ex.getMessage()));
            }
        }
    }

    private NotificationEvent persistInboundEvent(InboundEvent event) {
        NotificationEvent entity = new NotificationEvent();
        entity.setEventId(event.eventId());
        entity.setEventType(event.eventType());
        entity.setAggregateId(parseAggregateId(event));
        entity.setAggregateType(event.aggregateType() == null || event.aggregateType().isBlank() ? "Unknown" : event.aggregateType());
        entity.setEventPayload(event.payload());
        entity.setProcessed(false);
        entity.setCreatedAt(Instant.now());
        return eventRepository.save(entity);
    }

    private void markEventProcessed(NotificationEvent event, String processingError) {
        event.setProcessed(true);
        event.setProcessedAt(Instant.now());
        event.setProcessingError(processingError);
        eventRepository.save(event);
    }

    private Notification createNotificationForEvent(InboundEvent event) {
        EventTemplateSelection selection = selectTemplate(event.eventType());
        if (selection == null) {
            log.debug("Ignoring event type {}", event.eventType());
            return null;
        }

        Long userId = extractUserId(event.payload())
                .orElseThrow(() -> new IllegalArgumentException("Could not resolve userId from payload"));

        NotificationPreference preference = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> preferenceRepository.save(buildDefaultPreference(userId)));

        if (!isEligibleByPreference(preference, event.eventType(), selection.type())) {
            Notification unsubscribed = buildNotificationEntity(event, selection, userId, "");
            unsubscribed.setStatus(NotificationStatus.UNSUBSCRIBED);
            unsubscribed.setFailureReason("Notification suppressed by user preferences");
            Notification saved = notificationRepository.save(unsubscribed);
            appendLog(saved, "FAILED", Map.of("reason", "unsubscribed"));
            return saved;
        }

        return notificationRepository.save(buildNotificationEntity(event, selection, userId, resolveRecipientAddress(event.payload(), userId)));
    }

    private Notification buildNotificationEntity(InboundEvent event, EventTemplateSelection selection, Long userId, String recipientAddress) {
        NotificationTemplate template = templateRepository
                .findByNameAndTypeAndActiveTrue(selection.templateName(), selection.type())
                .orElseThrow(() -> new IllegalStateException("Missing active template: " + selection.templateName()));

        Map<String, Object> templateValues = payloadAsMap(event.payload());
        String body = templateRenderer.render(template.getBodyTemplate(), templateValues);
        String subject = template.getSubjectTemplate() == null ? null : templateRenderer.render(template.getSubjectTemplate(), templateValues);

        Notification notification = new Notification();
        notification.setNotificationId(UUID.randomUUID().toString());
        notification.setUserId(userId);
        notification.setType(selection.type());
        notification.setSubject(subject);
        notification.setBody(body);
        notification.setTemplateName(template.getName());
        notification.setTemplateVariables(objectMapper.valueToTree(templateValues));
        notification.setStatus(NotificationStatus.PENDING);
        notification.setRecipientAddress(recipientAddress);
        notification.setPriority(NotificationPriority.NORMAL);
        notification.setRetryCount(0);
        notification.setMaxRetries(3);
        notification.setMetadata(event.payload());
        notification.setCreatedAt(Instant.now());
        notification.setUpdatedAt(Instant.now());
        return notification;
    }

    private void trySendNotification(Notification notification, InboundEvent event) {
        if (notification.getStatus() == NotificationStatus.UNSUBSCRIBED) {
            return;
        }

        if (notification.getType() != NotificationType.EMAIL) {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setFailedAt(Instant.now());
            notification.setFailureReason("Only EMAIL channel is implemented in this slice");
            notification.setUpdatedAt(Instant.now());
            appendLog(notification, "FAILED", Map.of("reason", notification.getFailureReason()));
            createFailedNotification(notification, notification.getFailureReason(), null);
            eventPublisherPort.publish("EmailNotificationFailed", notification.getNotificationId(), buildNotificationPayload(notification));
            return;
        }

        try {
            emailSenderPort.send(new SendEmailCommand(
                    notification.getRecipientAddress(),
                    notification.getSubject(),
                    notification.getBody(),
                    true
            ));
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(Instant.now());
            notification.setUpdatedAt(Instant.now());
            appendLog(notification, "SENT", Map.of("eventId", event.eventId()));
            eventPublisherPort.publish("EmailNotificationSent", notification.getNotificationId(), buildNotificationPayload(notification));
        } catch (Exception ex) {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setFailedAt(Instant.now());
            notification.setFailureReason(ex.getMessage());
            notification.setNextRetryAt(Instant.now().plus(baseRetryDelayMinutes, ChronoUnit.MINUTES));
            notification.setUpdatedAt(Instant.now());
            appendLog(notification, "FAILED", Map.of("eventId", event.eventId(), "error", ex.getMessage()));
            createFailedNotification(notification, ex.getMessage(), ex);
            eventPublisherPort.publish("EmailNotificationFailed", notification.getNotificationId(), buildNotificationPayload(notification));
        }
    }

    private void createFailedNotification(Notification notification, String reason, Exception ex) {
        FailedNotification failed = new FailedNotification();
        failed.setNotification(notification);
        failed.setReason(reason == null ? "Unknown failure" : reason);
        failed.setErrorStacktrace(ex == null ? null : ex.toString());
        failed.setRetryCount(notification.getRetryCount());
        failed.setNextRetryTime(notification.getNextRetryAt());
        failed.setCreatedAt(Instant.now());
        failedNotificationRepository.save(failed);
    }

    private void appendLog(Notification notification, String action, Map<String, Object> details) {
        org.ecommerce.notificationservice.domain.model.NotificationLog logEntry = new org.ecommerce.notificationservice.domain.model.NotificationLog();
        logEntry.setNotification(notification);
        logEntry.setAction(action);
        logEntry.setDetails(objectMapper.valueToTree(details));
        logEntry.setTimestamp(Instant.now());
        notification.getLogs().add(logEntry);
    }

    private JsonNode buildNotificationPayload(Notification notification) {
        return objectMapper.valueToTree(Map.of(
                "notificationId", notification.getNotificationId(),
                "userId", notification.getUserId(),
                "type", notification.getType().name(),
                "status", notification.getStatus().name(),
                "recipientAddress", notification.getRecipientAddress() == null ? "" : notification.getRecipientAddress()
        ));
    }

    private String resolveRecipientAddress(JsonNode payload, Long userId) {
        if (payload != null && payload.hasNonNull("email")) {
            return payload.get("email").asText();
        }
        Optional<String> customerEmail = customerContactPort.resolveEmailByUserId(userId);
        if (customerEmail.isPresent()) {
            return customerEmail.get();
        }
        throw new IllegalArgumentException("Could not resolve email recipient for userId=" + userId);
    }

    private boolean isEligibleByPreference(NotificationPreference preference, String eventType, NotificationType type) {
        if (preference.isUnsubscribedFromAll()) {
            return false;
        }
        if (type != NotificationType.EMAIL) {
            return true;
        }

        return switch (eventType) {
            case "OrderCreated" -> preference.isEmailOnOrderCreated();
            case "OrderShipped" -> preference.isEmailOnOrderShipped();
            case "OrderDelivered" -> preference.isEmailOnOrderDelivered();
            case "PaymentCompleted" -> preference.isEmailOnPaymentSuccess();
            case "PaymentFailed" -> preference.isEmailOnPaymentFailed();
            case "InventoryAlert" -> preference.isEmailOnInventoryAlert();
            default -> true;
        };
    }

    private EventTemplateSelection selectTemplate(String eventType) {
        return switch (eventType) {
            case "OrderCreated" -> new EventTemplateSelection("order_confirmation", NotificationType.EMAIL);
            case "OrderShipped" -> new EventTemplateSelection("order_shipped", NotificationType.EMAIL);
            case "PaymentCompleted" -> new EventTemplateSelection("payment_success", NotificationType.EMAIL);
            case "PaymentFailed" -> new EventTemplateSelection("payment_failed", NotificationType.EMAIL);
            default -> null;
        };
    }

    private Optional<Long> extractUserId(JsonNode payload) {
        if (payload == null) {
            return Optional.empty();
        }
        List<String> keys = List.of("userId", "customerId", "user_id");
        for (String key : keys) {
            if (payload.hasNonNull(key)) {
                JsonNode value = payload.get(key);
                if (value.isNumber()) {
                    return Optional.of(value.asLong());
                }
                if (value.isTextual()) {
                    Matcher matcher = DIGITS_PATTERN.matcher(value.asText());
                    if (matcher.find()) {
                        return Optional.of(Long.parseLong(matcher.group(1)));
                    }
                }
            }
        }
        return Optional.empty();
    }

    private Long parseAggregateId(InboundEvent event) {
        if (event.aggregateId() != null) {
            Matcher matcher = DIGITS_PATTERN.matcher(event.aggregateId());
            if (matcher.find()) {
                return Long.parseLong(matcher.group(1));
            }
        }
        if (event.payload() != null && event.payload().hasNonNull("orderId")) {
            JsonNode orderId = event.payload().get("orderId");
            if (orderId.isNumber()) {
                return orderId.asLong();
            }
            Matcher matcher = DIGITS_PATTERN.matcher(orderId.asText());
            if (matcher.find()) {
                return Long.parseLong(matcher.group(1));
            }
        }
        return 0L;
    }

    private InboundEvent parseInboundEvent(String rawEvent) {
        try {
            InboundEventMessage message = objectMapper.readValue(rawEvent, InboundEventMessage.class);
            return new InboundEvent(
                    message.eventId(),
                    message.eventType(),
                    message.aggregateId(),
                    message.aggregateType(),
                    message.payload(),
                    message.source()
            );
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Failed to parse inbound event payload", ex);
        }
    }

    private Map<String, Object> payloadAsMap(JsonNode payload) {
        if (payload == null || payload.isNull()) {
            return Map.of();
        }
        return objectMapper.convertValue(payload, Map.class);
    }

    private NotificationPreference buildDefaultPreference(Long userId) {
        NotificationPreference preference = new NotificationPreference();
        preference.setUserId(userId);
        preference.setEmailOnOrderCreated(true);
        preference.setEmailOnOrderShipped(true);
        preference.setEmailOnOrderDelivered(true);
        preference.setEmailOnPaymentSuccess(true);
        preference.setEmailOnPaymentFailed(true);
        preference.setEmailOnInventoryAlert(false);
        preference.setSmsOnOrderShipped(false);
        preference.setSmsOnPaymentFailed(true);
        preference.setPushOnOrderUpdate(true);
        preference.setPushOnPaymentUpdate(true);
        preference.setUnsubscribedFromMarketing(false);
        preference.setUnsubscribedFromAll(false);
        Instant now = Instant.now();
        preference.setCreatedAt(now);
        preference.setUpdatedAt(now);
        return preference;
    }

    private record EventTemplateSelection(String templateName, NotificationType type) {
    }
}


