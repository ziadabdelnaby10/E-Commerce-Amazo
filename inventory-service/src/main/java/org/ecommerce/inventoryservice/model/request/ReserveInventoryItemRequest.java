package org.ecommerce.inventoryservice.model.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReserveInventoryItemRequest(
        @NotNull Long productId,
        @NotNull @Min(1) Integer quantity
) {
}

