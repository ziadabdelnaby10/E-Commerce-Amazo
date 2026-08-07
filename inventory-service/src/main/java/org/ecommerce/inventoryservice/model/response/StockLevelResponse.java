package org.ecommerce.inventoryservice.model.response;

import java.time.Instant;

public record StockLevelResponse(
        Long id,
        Long productId,
        Integer quantityAvailable,
        Integer quantityReserved,
        Integer quantityDamaged,
        Integer totalQuantity,
        String warehouseLocation,
        Instant lastCountedAt,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {
}

