package org.ecommerce.inventoryservice.model.response;

import org.ecommerce.inventoryservice.model.entity.ProductStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
        Long id,
        String sku,
        String name,
        String description,
        String category,
        BigDecimal price,
        BigDecimal cost,
        Long supplierId,
        Integer reorderLevel,
        Integer reorderQuantity,
        ProductStatus status,
        Instant createdAt,
        Instant updatedAt,
        Long version,
        SimpleStockLevelResponse stockLevel
) {
}

