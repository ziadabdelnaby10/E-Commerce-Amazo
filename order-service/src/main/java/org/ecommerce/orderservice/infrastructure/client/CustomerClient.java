package org.ecommerce.orderservice.infrastructure.client;

import org.ecommerce.orderservice.infrastructure.client.dto.CustomerResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Resolved through Eureka: {@code name} is the service id and {@code path} carries the target's
 * servlet context path plus controller mapping, so no host or port is hardcoded.
 */
@FeignClient(name = "customer-service", path = "/api/v1/customers")
public interface CustomerClient {

    @GetMapping("/exists/{customerId}")
    Boolean existsById(@PathVariable String customerId);

    @GetMapping("/{customerId}")
    CustomerResponse findById(@PathVariable String customerId);
}
