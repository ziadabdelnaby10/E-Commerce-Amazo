package org.ecommerce.gatewayservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the API gateway.
 *
 * <p>This service is the public front door of the microservice platform. It applies
 * cross-cutting concerns such as route matching, JWT validation, claim relaying,
 * fallback handling, and circuit breaking before proxying requests to internal services.</p>
 *
 * <p>Routes are defined through Spring Cloud Gateway and resolved via Eureka using
 * {@code lb://} URIs, which keeps routing independent of concrete host/port values.</p>
 */
@SpringBootApplication
public class GatewayServiceApplication {

    /**
     * Starts the gateway application.
     *
     * @param args standard Spring Boot startup arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }

}
