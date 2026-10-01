package org.ecommerce.discoveryservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Bootstrap class for the Eureka service registry.
 *
 * <p>This service acts as the discovery hub for the microservice system. Runtime
 * clients such as the API gateway and downstream services register themselves here
 * and resolve peer services by logical name instead of fixed host/port pairs.</p>
 *
 * <p>That indirection is what allows the project to use load-balanced URIs such as
 * {@code lb://customer-service} and to scale services without rewriting route definitions.</p>
 */
@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServiceApplication {

    /**
     * Starts the Eureka server.
     *
     * @param args standard Spring Boot startup arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServiceApplication.class, args);
    }

}
