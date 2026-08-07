package org.ecommerce.inventoryservice.model.request;

import org.ecommerce.inventoryservice.model.entity.ProductStatus;

import java.math.BigDecimal;

public record ProductUpdateRequest(
        String sku,
        String name,
        String description,
        String category,
        BigDecimal price,
        BigDecimal cost,
        ProductStatus status,
        Long supplierId,
        Integer reorderLevel,
        Integer reorderQuantity,
        Integer initialQuantity,
        String warehouseLocation
) {
}

