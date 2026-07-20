package org.ecommerce.gatewayservice.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class FallbackControllerTest {

    private final FallbackController fallbackController = new FallbackController();

    @Test
    void shouldReturnServiceUnavailableForCustomerFallback() {
        var response = fallbackController.customersFallback();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isEqualTo("Customer Service is temporarily unavailable");
    }
}


