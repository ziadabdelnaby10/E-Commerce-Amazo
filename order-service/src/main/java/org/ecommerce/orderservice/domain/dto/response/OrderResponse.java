package org.ecommerce.orderservice.domain.dto.response;

import org.ecommerce.orderservice.domain.dto.AddressDto;
import org.ecommerce.orderservice.domain.model.OrderStatus;
import org.ecommerce.orderservice.domain.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        String orderNumber,
        String userId,
        OrderStatus status,
        PaymentStatus paymentStatus,
        BigDecimal totalAmount,
        String currency,
        AddressDto shippingAddress,
        AddressDto billingAddress,
        String notes,
        Instant createdAt,
        Instant updatedAt,
        List<OrderItemResponse> items
) {
}

