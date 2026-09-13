package org.ecommerce.gatewayservice.controller;

import org.ecommerce.gatewayservice.response.GeneralResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Responses returned when a route's circuit breaker is open.
 *
 * <p>Mapped with {@code @RequestMapping} rather than {@code @GetMapping} because the gateway
 * forwards the original request method: a failed {@code POST /api/v1/orders} arrives here as a
 * POST, and a GET-only mapping would turn the intended 503 into a confusing 405.</p>
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/customers")
    public ResponseEntity<GeneralResponse<String>> customersFallback() {
        return unavailable("Customer Service is temporarily unavailable");
    }

    @RequestMapping("/inventory")
    public ResponseEntity<GeneralResponse<String>> inventoryFallback() {
        return unavailable("Inventory Service is temporarily unavailable");
    }

    @RequestMapping("/orders")
    public ResponseEntity<GeneralResponse<String>> ordersFallback() {
        return unavailable("Order Service is temporarily unavailable");
    }

    @RequestMapping("/payments")
    public ResponseEntity<GeneralResponse<String>> paymentsFallback() {
        return unavailable("Payment Service is temporarily unavailable");
    }

    private ResponseEntity<GeneralResponse<String>> unavailable(String message) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(GeneralResponse.of(HttpStatus.SERVICE_UNAVAILABLE.value(), message));
    }
}
