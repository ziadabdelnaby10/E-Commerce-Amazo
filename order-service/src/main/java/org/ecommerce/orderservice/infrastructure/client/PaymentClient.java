package org.ecommerce.orderservice.infrastructure.client;

import org.ecommerce.orderservice.infrastructure.client.dto.InitiatePaymentRequest;
import org.ecommerce.orderservice.infrastructure.client.dto.InitiatePaymentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "paymentClient", url = "${application.config.payment-url}")
public interface PaymentClient {

    @PostMapping
    InitiatePaymentResponse initiatePayment(@RequestBody InitiatePaymentRequest request);
}

