package org.ecommerce.inventoryservice.model.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank String sku,
        @NotBlank String name,
        String description,
        String category,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal price,
        BigDecimal cost,
        Long supplierId,
        Integer reorderLevel,
        Integer reorderQuantity,
        Integer initialQuantity,
        String warehouseLocation
) {
}

