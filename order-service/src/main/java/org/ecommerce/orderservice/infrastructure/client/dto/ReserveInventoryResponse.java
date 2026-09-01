package org.ecommerce.orderservice.infrastructure.client.dto;

public record ReserveInventoryResponse(
        boolean reserved,
        String reason
) {

    public static ReserveInventoryResponse success() {
        return new ReserveInventoryResponse(true, null);
    }

    public static ReserveInventoryResponse failed(String reason) {
        return new ReserveInventoryResponse(false, reason);
    }
}

