package org.ecommerce.orderservice.infrastructure.client;

import org.ecommerce.orderservice.infrastructure.client.dto.ReleaseInventoryRequest;
import org.ecommerce.orderservice.infrastructure.client.dto.ReserveInventoryRequest;
import org.ecommerce.orderservice.infrastructure.client.dto.ReserveInventoryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "inventoryClient", url = "${application.config.inventory-url}")
public interface InventoryClient {

    @PostMapping("/reservations")
    ReserveInventoryResponse reserveInventory(@RequestBody ReserveInventoryRequest request);

    @PostMapping("/reservations/release")
    void releaseInventory(@RequestBody ReleaseInventoryRequest request);
}

