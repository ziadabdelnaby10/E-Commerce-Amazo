package org.ecommerce.inventoryservice.event;

import org.ecommerce.inventoryservice.model.entity.ProductStatus;
import org.ecommerce.inventoryservice.model.request.StockAdjustmentRequest;

import java.util.Objects;

public record StockAdjustedEvent(
        Long productId,
        ProductStatus targetStatus,
        StockAdjustmentRequest request
) {
    public StockAdjustedEvent {
        Objects.requireNonNull(productId, "productId must not be null");
        Objects.requireNonNull(targetStatus, "targetStatus must not be null");
        Objects.requireNonNull(request, "request must not be null");
    }
}

