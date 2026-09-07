package org.ecommerce.gatewayservice.controller;

import org.ecommerce.gatewayservice.response.GeneralResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/customers")
    public ResponseEntity<GeneralResponse<String>> customersFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(GeneralResponse.of(HttpStatus.SERVICE_UNAVAILABLE.value(), "Customer Service is temporarily unavailable"));
    }

    @GetMapping("/inventory")
    public ResponseEntity<GeneralResponse<String>> inventoryFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(GeneralResponse.of(HttpStatus.SERVICE_UNAVAILABLE.value(), "Inventory Service is temporarily unavailable"));
    }

    @GetMapping("/orders")
    public ResponseEntity<GeneralResponse<String>> ordersFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(GeneralResponse.of(HttpStatus.SERVICE_UNAVAILABLE.value(), "Order Service is temporarily unavailable"));
    }
}

