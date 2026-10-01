package org.ecommerce.orderservice.service.implementation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ecommerce.orderservice.domain.dto.request.CreateOrderRequest;
import org.ecommerce.orderservice.domain.dto.response.OrderResponse;
import org.ecommerce.orderservice.domain.dto.response.OrderSummaryResponse;
import org.ecommerce.orderservice.exception.IdempotencyKeyInProgressException;
import org.ecommerce.orderservice.exception.OrderNotFoundException;
import org.ecommerce.orderservice.service.OrderService;
import org.ecommerce.orderservice.infrastructure.client.OrderDependencyGateway;
import org.ecommerce.orderservice.infrastructure.client.dto.InitiatePaymentResponse;
import org.ecommerce.orderservice.infrastructure.client.dto.CustomerResponse;
import org.ecommerce.orderservice.infrastructure.client.dto.ReserveInventoryResponse;
import org.ecommerce.orderservice.domain.model.IdempotencyKey;
import org.ecommerce.orderservice.domain.model.Order;
import org.ecommerce.orderservice.domain.model.OrderEvent;
import org.ecommerce.orderservice.domain.model.OrderStatus;
import org.ecommerce.orderservice.domain.model.OrderStatusHistory;
import org.ecommerce.orderservice.domain.model.PaymentStatus;
import org.ecommerce.orderservice.infrastructure.mapping.OrderMapper;
import org.ecommerce.orderservice.infrastructure.persistence.repository.IdempotencyKeyJpaRepository;
import org.ecommerce.orderservice.infrastructure.persistence.repository.OrderEventJpaRepository;
import org.ecommerce.orderservice.infrastructure.persistence.repository.OrderJpaRepository;
import org.ecommerce.orderservice.infrastructure.persistence.repository.OrderStatusHistoryJpaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Default implementation of order orchestration.
 *
 * <p>This service coordinates order creation, idempotency reservation, inventory reservation,
 * payment initiation, order-status history tracking, and outbox event persistence for later
 * Kafka publication.</p>
 *
 * <p>The flow intentionally checks already-processed idempotency keys before any downstream
 * network call so exact retries can return the cached response immediately.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class OrderServiceImpl implements OrderService {

    private static final String CREATE_ORDER_ENDPOINT = "/api/v1/orders";

    private final OrderJpaRepository orderRepository;
    private final OrderEventJpaRepository orderEventRepository;
    private final OrderStatusHistoryJpaRepository statusHistoryRepository;
    private final IdempotencyKeyJpaRepository idempotencyRepository;
    private final OrderMapper orderMapper;
    private final OrderDependencyGateway dependencyGateway;
    private final ObjectMapper objectMapper;

    /**
     * Creates an order and coordinates the first steps of the order saga.
     *
     * @param userId authenticated user identifier propagated from the gateway/JWT
     * @param idempotencyKey client-supplied key used to make retried requests safe
     * @param request order creation payload
     * @return created order response, or the previously cached response for a completed idempotent request
     */
    @Override
    @Transactional
    public OrderResponse createOrder(String userId, String idempotencyKey, CreateOrderRequest request) {

        //checkIfUserExist
        var customer = dependencyGateway.findCustomer(userId);
        if(customer.isEmpty()) {
            throw new EntityNotFoundException("User not found with id " + userId);
        }

        IdempotencyKey existing = idempotencyRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) {
            if (existing.getResponseBody() == null) {
                throw new IdempotencyKeyInProgressException(idempotencyKey);
            }
            return objectMapper.convertValue(existing.getResponseBody(), OrderResponse.class);
        }

        // A brand-new order must belong to a real customer, but we only need the full customer
        // projection opportunistically to snapshot their email into the order/event payload.
        if (!Boolean.TRUE.equals(dependencyGateway.checkCustomerExist(userId))) {
            throw new EntityNotFoundException("User not found with id " + userId);
        }

        IdempotencyKey keyRecord = reserveIdempotencyKey(userId, idempotencyKey, request);

        Order order = orderMapper.toOrder(request);
        order.setUserId(userId);
        order.setCustomerEmail(customer
                .map(CustomerResponse::email)
                .orElse(null));
        order.setOrderNumber(generateOrderNumber());
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setCurrency("USD");
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());

        // Ensure both sides of the relationship are set before persist.
        order.getItems().forEach(item -> {
            item.setOrder(order);
            item.setCreatedAt(Instant.now());
        });

        order.setTotalAmount(calculateTotal(order));

        Order saved = orderRepository.save(order);

        statusHistoryRepository.save(buildHistory(saved, null, OrderStatus.PENDING, "SYSTEM", "Order created"));

        orderEventRepository.save(buildOrderEvent(saved, "OrderCreated"));

        ReserveInventoryResponse reservation = dependencyGateway.reserveInventory(saved);
        if (!reservation.reserved()) {
            applyCancellation(saved, "inventory-service", reservation.reason() == null ? "Inventory reservation failed" : reservation.reason());
        } else {
            InitiatePaymentResponse payment = dependencyGateway.initiatePayment(saved);
            if (!payment.accepted()) {
                dependencyGateway.releaseInventory(saved);
                applyCancellation(saved, "payment-service", payment.reason() == null ? "Payment initiation failed" : payment.reason());
            }
        }

        OrderResponse response = orderMapper.toResponse(saved);
        keyRecord.setResponseBody(objectMapper.valueToTree(response));
        keyRecord.setResponseStatus(201);
        idempotencyRepository.save(keyRecord);

        return response;
    }

    /**
     * Retrieves a single order with its items.
     */
    @Override
    public OrderResponse getById(Long orderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        return orderMapper.toResponse(order);
    }

    /**
     * Lists order summaries for a user, optionally filtered by status.
     */
    @Override
    public Page<OrderSummaryResponse> listByUser(Long userId, OrderStatus status, Pageable pageable) {
        return (status == null
                ? orderRepository.findSummariesByUser(userId, pageable)
                : orderRepository.findSummariesByUserAndStatus(userId, status, pageable))
                .map(orderMapper::toOrderSummaryResponse);
    }

    /**
     * Marks an order as paid when the payment service emits a successful payment event.
     */
    @Transactional
    @Override
    public void markPaymentCaptured(Long orderId, String eventId) {
        if (orderEventRepository.existsByEventId(eventId)) {
            return;
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        OrderStatus oldStatus = order.getStatus();

        order.setPaymentStatus(PaymentStatus.CAPTURED);
        order.setStatus(OrderStatus.CONFIRMED);
        order.setUpdatedAt(Instant.now());

        statusHistoryRepository.save(buildHistory(order, oldStatus, OrderStatus.CONFIRMED, "payment-service", "Payment completed"));
        orderEventRepository.save(buildExternalEvent(order, eventId, "PaymentCompleted"));
    }

    /**
     * Marks an order as cancelled when payment processing fails.
     */
    @Transactional
    @Override
    public void markPaymentFailed(Long orderId, String eventId) {
        if (orderEventRepository.existsByEventId(eventId)) {
            return;
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        OrderStatus oldStatus = order.getStatus();

        order.setPaymentStatus(PaymentStatus.FAILED);
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(Instant.now());
        order.setUpdatedAt(Instant.now());

        statusHistoryRepository.save(buildHistory(order, oldStatus, OrderStatus.CANCELLED, "payment-service", "Payment failed"));
        orderEventRepository.save(buildExternalEvent(order, eventId, "PaymentFailed"));
        orderEventRepository.save(buildOrderEvent(order, "OrderCancelled"));
    }

    /**
     * Reserves an idempotency slot for a new create-order request.
     *
     * <p>If another request wins the unique-key race first, the existing record is read back so
     * the caller can decide whether the original request is still in progress or already completed.</p>
     */
    private IdempotencyKey reserveIdempotencyKey(String userId, String idempotencyKey, CreateOrderRequest request) {
        IdempotencyKey keyRecord = new IdempotencyKey();
        keyRecord.setIdempotencyKey(idempotencyKey);
        keyRecord.setUserId(userId);
        keyRecord.setEndpoint(CREATE_ORDER_ENDPOINT);
        keyRecord.setMethod("POST");
        keyRecord.setRequestBody(objectMapper.valueToTree(request));
        keyRecord.setCreatedAt(Instant.now());
        keyRecord.setExpiresAt(Instant.now().plus(Duration.ofHours(24)));

        try {
            return idempotencyRepository.save(keyRecord);
        } catch (DataIntegrityViolationException ex) {
            return idempotencyRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> ex);
        }
    }

    /**
     * Calculates the order total from the current item set.
     */
    private BigDecimal calculateTotal(Order order) {
        return order.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Generates a human-readable order number independent of the database id.
     */
    private String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Creates a status-history record for audit and timeline purposes.
     */
    private OrderStatusHistory buildHistory(Order order, OrderStatus oldStatus, OrderStatus newStatus, String changedBy, String reason) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setChangedBy(changedBy);
        history.setReason(reason);
        history.setCreatedAt(Instant.now());
        return history;
    }

    /**
     * Creates an outbox event that will be published to Kafka by the scheduled outbox publisher.
     */
    private OrderEvent buildOrderEvent(Order order, String eventType) {
        OrderEvent event = new OrderEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setOrder(order);
        event.setEventType(eventType);
        event.setEventPayload(buildEventPayload(order));
        // Outbox events are persisted first and marked published later by the scheduled publisher.
        event.setPublishedToKafka(false);
        event.setCreatedAt(Instant.now());
        return event;
    }

    /**
     * Records an already-published external event inside the local order event stream.
     */
    private OrderEvent buildExternalEvent(Order order, String eventId, String eventType) {
        OrderEvent event = new OrderEvent();
        event.setEventId(eventId);
        event.setOrder(order);
        event.setEventType(eventType);
        event.setEventPayload(buildEventPayload(order));
        event.setPublishedToKafka(true);
        event.setCreatedAt(Instant.now());
        return event;
    }

    /**
     * Builds the event payload as the order projection plus the recipient email.
     *
     * <p>The email makes the event self-contained: notification-service consumes it from a Kafka
     * thread that has no caller token, so it cannot look the address up over HTTP.</p>
     */
    private JsonNode buildEventPayload(Order order) {
        ObjectNode payload = objectMapper.valueToTree(orderMapper.toResponse(order));
        payload.put("userId", order.getUserId());
        payload.put("email", order.getCustomerEmail());
        return payload;
    }

    /**
     * Applies a terminal cancellation state when a downstream dependency rejects the workflow.
     */
    private void applyCancellation(Order order, String changedBy, String reason) {
        OrderStatus oldStatus = order.getStatus();
        order.setPaymentStatus(PaymentStatus.FAILED);
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        statusHistoryRepository.save(buildHistory(order, oldStatus, OrderStatus.CANCELLED, changedBy, reason));
        orderEventRepository.save(buildOrderEvent(order, "OrderCancelled"));
    }

//    private OrderSummaryResponse toSummary(OrderSummaryProjection summary) {
//        return new OrderSummaryResponse(
//                summary.getId(),
//                summary.getOrderNumber(),
//                summary.getStatus(),
//                summary.getPaymentStatus(),
//                summary.getTotalAmount(),
//                summary.getCurrency(),
//                summary.getCreatedAt()
//        );
//    }
}

