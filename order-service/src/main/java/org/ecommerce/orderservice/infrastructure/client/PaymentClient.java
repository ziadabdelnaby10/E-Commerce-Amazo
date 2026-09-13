package org.ecommerce.orderservice.infrastructure.client;

import org.ecommerce.orderservice.infrastructure.client.dto.InitiatePaymentRequest;
import org.ecommerce.orderservice.infrastructure.client.dto.InitiatePaymentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Resolved through Eureka: {@code name} is the service id and {@code path} carries the target's
 * servlet context path plus controller mapping, so no host or port is hardcoded.
 */
@FeignClient(name = "payment-service", path = "/api/v1/payments")
public interface PaymentClient {

    @PostMapping
    InitiatePaymentResponse initiatePayment(@RequestBody InitiatePaymentRequest request);
}

