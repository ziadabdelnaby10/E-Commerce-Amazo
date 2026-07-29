package org.ecommerce.inventoryservice.model.response;

import java.time.Instant;

public record LowStockAlertResponse(
        Long id,
        Long productId,
        String productSku,
        Integer currentQuantity,
        Integer reorderLevel,
        Instant alertSentAt,
        Boolean purchaseOrderCreated,
        Long purchaseOrderId,
        Instant createdAt,
        Instant resolvedAt
) {
}

