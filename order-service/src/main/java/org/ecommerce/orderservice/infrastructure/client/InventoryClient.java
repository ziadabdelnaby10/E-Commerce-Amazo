package org.ecommerce.orderservice.infrastructure.client;

import org.ecommerce.orderservice.infrastructure.client.dto.ReleaseInventoryRequest;
import org.ecommerce.orderservice.infrastructure.client.dto.ReserveInventoryRequest;
import org.ecommerce.orderservice.infrastructure.client.dto.ReserveInventoryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Resolved through Eureka: {@code name} is the service id and {@code path} carries the target's
 * servlet context path plus controller mapping, so no host or port is hardcoded.
 */
@FeignClient(name = "inventory-service", path = "/api/v1/inventory")
public interface InventoryClient {

    @PostMapping("/reservations")
    ReserveInventoryResponse reserveInventory(@RequestBody ReserveInventoryRequest request);

    @PostMapping("/reservations/release")
    void releaseInventory(@RequestBody ReleaseInventoryRequest request);
}

