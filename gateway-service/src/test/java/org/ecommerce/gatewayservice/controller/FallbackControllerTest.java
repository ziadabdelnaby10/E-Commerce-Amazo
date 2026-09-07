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
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().statusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
        assertThat(response.getBody().data()).isEqualTo("Customer Service is temporarily unavailable");
        assertThat(response.getBody().time()).isNotNull();
    }
}


