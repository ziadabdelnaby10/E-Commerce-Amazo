package org.ecommerce.inventoryservice.model.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.ecommerce.inventoryservice.model.entity.ProductStatus;

import java.math.BigDecimal;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
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
        @JsonInclude(JsonInclude.Include.NON_NULL)
        SimpleStockLevelResponse stockLevel
) {
}

