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

    /**
     * Fallback response for customer-service routes.
     *
     * @return standardized service unavailable response
     */
    @RequestMapping("/customers")
    public ResponseEntity<GeneralResponse<String>> customersFallback() {
        return unavailable("Customer Service is temporarily unavailable");
    }

    /**
     * Fallback response for inventory-service routes.
     *
     * @return standardized service unavailable response
     */
    @RequestMapping("/inventory")
    public ResponseEntity<GeneralResponse<String>> inventoryFallback() {
        return unavailable("Inventory Service is temporarily unavailable");
    }

    /**
     * Fallback response for order-service routes.
     *
     * @return standardized service unavailable response
     */
    @RequestMapping("/orders")
    public ResponseEntity<GeneralResponse<String>> ordersFallback() {
        return unavailable("Order Service is temporarily unavailable");
    }

    /**
     * Fallback response for payment-service routes.
     *
     * @return standardized service unavailable response
     */
    @RequestMapping("/payments")
    public ResponseEntity<GeneralResponse<String>> paymentsFallback() {
        return unavailable("Payment Service is temporarily unavailable");
    }

    /**
     * Builds a common 503 response payload for circuit-breaker fallbacks.
     *
     * @param message service-specific fallback message
     * @return HTTP 503 response body
     */
    private ResponseEntity<GeneralResponse<String>> unavailable(String message) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(GeneralResponse.of(HttpStatus.SERVICE_UNAVAILABLE.value(), message));
    }
}
