package org.ecommerce.orderservice.service.implementation;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ecommerce.orderservice.domain.dto.request.CreateOrderRequest;
import org.ecommerce.orderservice.domain.dto.response.OrderResponse;
import org.ecommerce.orderservice.domain.dto.response.OrderSummaryResponse;
import org.ecommerce.orderservice.exception.IdempotencyKeyInProgressException;
import org.ecommerce.orderservice.exception.OrderNotFoundException;
import org.ecommerce.orderservice.infrastructure.client.dto.CustomerResponse;
import org.ecommerce.orderservice.service.OrderService;
import org.ecommerce.orderservice.infrastructure.client.OrderDependencyGateway;
import org.ecommerce.orderservice.infrastructure.client.dto.InitiatePaymentResponse;
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

    @Override
    @Transactional
    public OrderResponse createOrder(String userId, String idempotencyKey, CreateOrderRequest request) {

        //checkIfUserExist
        Boolean customer = dependencyGateway.checkCustomerExist(userId);
        if(customer == null || !customer) {
            throw new EntityNotFoundException("User not found with id " + userId);
        }

        IdempotencyKey existing = idempotencyRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) {
            if (existing.getResponseBody() == null) {
                throw new IdempotencyKeyInProgressException(idempotencyKey);
            }
            return objectMapper.convertValue(existing.getResponseBody(), OrderResponse.class);
        }

        IdempotencyKey keyRecord = reserveIdempotencyKey(userId, idempotencyKey, request);

        Order order = orderMapper.toOrder(request);
        order.setUserId(userId);
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

        orderEventRepository.save(buildOrderEvent(saved, "OrderCreated", false));

//        ReserveInventoryResponse reservation = dependencyGateway.reserveInventory(saved);
//        if (!reservation.reserved()) {
//            applyCancellation(saved, "inventory-service", reservation.reason() == null ? "Inventory reservation failed" : reservation.reason());
//        } else {
//            InitiatePaymentResponse payment = dependencyGateway.initiatePayment(saved);
//            if (!payment.accepted()) {
//                dependencyGateway.releaseInventory(saved);
//                applyCancellation(saved, "payment-service", payment.reason() == null ? "Payment initiation failed" : payment.reason());
//            }
//        }

        OrderResponse response = orderMapper.toResponse(saved);
        keyRecord.setResponseBody(objectMapper.valueToTree(response));
        keyRecord.setResponseStatus(201);
        idempotencyRepository.save(keyRecord);

        return response;
    }

    @Override
    public OrderResponse getById(Long orderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        return orderMapper.toResponse(order);
    }

    @Override
    public Page<OrderSummaryResponse> listByUser(Long userId, OrderStatus status, Pageable pageable) {
        return (status == null
                ? orderRepository.findSummariesByUser(userId, pageable)
                : orderRepository.findSummariesByUserAndStatus(userId, status, pageable))
                .map(orderMapper::toOrderSummaryResponse);
    }

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
        orderEventRepository.save(buildOrderEvent(order, "OrderCancelled", false));
    }

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

    private BigDecimal calculateTotal(Order order) {
        return order.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

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

    private OrderEvent buildOrderEvent(Order order, String eventType, boolean publishedToKafka) {
        OrderEvent event = new OrderEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setOrder(order);
        event.setEventType(eventType);
        event.setEventPayload(objectMapper.valueToTree(orderMapper.toResponse(order)));
        event.setPublishedToKafka(publishedToKafka);
        event.setCreatedAt(Instant.now());
        return event;
    }

    private OrderEvent buildExternalEvent(Order order, String eventId, String eventType) {
        OrderEvent event = new OrderEvent();
        event.setEventId(eventId);
        event.setOrder(order);
        event.setEventType(eventType);
        event.setEventPayload(objectMapper.valueToTree(orderMapper.toResponse(order)));
        event.setPublishedToKafka(true);
        event.setCreatedAt(Instant.now());
        return event;
    }

    private void applyCancellation(Order order, String changedBy, String reason) {
        OrderStatus oldStatus = order.getStatus();
        order.setPaymentStatus(PaymentStatus.FAILED);
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        statusHistoryRepository.save(buildHistory(order, oldStatus, OrderStatus.CANCELLED, changedBy, reason));
        orderEventRepository.save(buildOrderEvent(order, "OrderCancelled", false));
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

