package org.ecommerce.inventoryservice.model.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StockAdjustmentRequest(
        @NotNull @Min(value = 1) Integer quantity,
        String transactionType,
        String referenceId,
        String referenceType,
        String reason,
        String createdBy,
        String notes
) {
}

