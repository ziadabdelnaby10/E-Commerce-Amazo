package org.ecommerce.inventoryservice.model.response;

import java.time.Instant;

public record InventoryTransactionResponse(
        Long id,
        Long productId,
        String transactionType,
        Integer quantity,
        String referenceId,
        String referenceType,
        String reason,
        String createdBy,
        String notes,
        Instant createdAt
) {
}

