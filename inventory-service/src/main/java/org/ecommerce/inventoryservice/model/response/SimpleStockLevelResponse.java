package org.ecommerce.inventoryservice.model.response;

import java.time.Instant;

public record SimpleStockLevelResponse(
        Integer quantityAvailable,
        Integer quantityReserved,
        Integer quantityDamaged,
        Integer totalQuantity,
        String warehouseLocation,
        Instant lastCountedAt,
        Long version
) {
}

