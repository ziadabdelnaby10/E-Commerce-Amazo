package org.ecommerce.orderservice.infrastructure.client.dto;

public record ReserveInventoryItemRequest(
        Long productId,
        Integer quantity
) {
}

