package org.ecommerce.inventoryservice.model.filter;

import lombok.Data;
import org.ecommerce.inventoryservice.model.entity.ProductStatus;

import java.math.BigDecimal;


public record ProductFilter(
        ProductStatus status,
        String sku,
        String name,
        String category,
        Long supplierId,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        String sortBy,
        String sortDirection
) {
}

