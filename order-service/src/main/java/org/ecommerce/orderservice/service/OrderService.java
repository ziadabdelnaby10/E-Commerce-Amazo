package org.ecommerce.orderservice.service;

import org.ecommerce.orderservice.domain.dto.request.CreateOrderRequest;
import org.ecommerce.orderservice.domain.dto.response.OrderResponse;
import org.ecommerce.orderservice.domain.dto.response.OrderSummaryResponse;
import org.ecommerce.orderservice.domain.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {
    OrderResponse createOrder(String userId, String idempotencyKey, CreateOrderRequest request);

    OrderResponse getById(Long orderId);

    Page<OrderSummaryResponse> listByUser(Long userId, OrderStatus status, Pageable pageable);

    void markPaymentCaptured(Long orderId, String eventId);

    void markPaymentFailed(Long orderId, String eventId);
}
