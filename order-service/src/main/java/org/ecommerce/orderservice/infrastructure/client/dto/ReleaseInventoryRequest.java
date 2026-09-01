package org.ecommerce.orderservice.infrastructure.client.dto;

import java.util.List;

public record ReleaseInventoryRequest(
        Long orderId,
        List<ReserveInventoryItemRequest> items
) {
}

