package org.ecommerce.inventoryservice.model.request;

import jakarta.validation.constraints.NotNull;
import org.ecommerce.inventoryservice.model.entity.ProductStatus;

public record ProductStatusUpdateRequest(@NotNull ProductStatus status) {
}

