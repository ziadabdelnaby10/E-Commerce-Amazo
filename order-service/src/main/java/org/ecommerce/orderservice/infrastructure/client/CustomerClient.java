package org.ecommerce.orderservice.infrastructure.client;

import org.ecommerce.orderservice.infrastructure.client.dto.CustomerResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "customerClient", url = "${application.config.customer-url}")
public interface CustomerClient {

    @GetMapping("/exists/{customerId}")
    Boolean existsById(@PathVariable String customerId);

    @GetMapping("/{customerId}")
    CustomerResponse findById(@PathVariable String customerId);
}
