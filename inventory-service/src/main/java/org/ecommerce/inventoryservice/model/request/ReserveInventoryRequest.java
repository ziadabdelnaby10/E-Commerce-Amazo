package org.ecommerce.inventoryservice.model.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ReserveInventoryRequest(
        @NotNull Long orderId,
        @NotEmpty List<@Valid ReserveInventoryItemRequest> items
) {
}

