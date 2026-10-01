package org.ecommerce.orderservice.service;

import org.ecommerce.orderservice.domain.dto.request.CreateOrderRequest;
import org.ecommerce.orderservice.domain.dto.response.OrderResponse;
import org.ecommerce.orderservice.domain.dto.response.OrderSummaryResponse;
import org.ecommerce.orderservice.domain.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Order application service contract.
 */
public interface OrderService {

    /**
     * Creates a new order or returns a cached result when the idempotency key was already processed.
     */
    OrderResponse createOrder(String userId, String idempotencyKey, CreateOrderRequest request);

    /** @return a detailed view of the order with the given id */
    OrderResponse getById(Long orderId);

    /** @return paged order summaries for a user, optionally filtered by status */
    Page<OrderSummaryResponse> listByUser(Long userId, OrderStatus status, Pageable pageable);

    /** Marks an order as paid from a payment-success event. */
    void markPaymentCaptured(Long orderId, String eventId);

    /** Marks an order as failed/cancelled from a payment-failure event. */
    void markPaymentFailed(Long orderId, String eventId);
}
