package org.ecommerce.orderservice.infrastructure.client.dto;

import java.util.List;

public record ReserveInventoryRequest(
        Long orderId,
        List<ReserveInventoryItemRequest> items
) {
}

